package com.paper.ch2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WK {
	
	static int N = 1;		//用户个数
	static int n = 7 + 2;	//组件个数
	
	static double B = 0;		//网络带宽
	static int nch = 2;			//网络子信道个数
	static double deltaB = 0;	//每个信道的带宽。（数据上传带宽，数据下载带宽）
	
	static int k = 0;		//MEC服务器个数
	static int r = 0;		//每个服务器的核数
	
	double T = 0;			//初始时间片[0,T]，T为所有用户的最大完成时间
	
	static User[] user = new User[N+1];				//所有用户，0为空下标
	
	CompListNode compList;		//计算资源占用链表的头节点
	
	NetListNode netList;		//网络资源占用链表的头节点
	
	// 主函数
	public static void main(String[] args) {
		WK wk = new WK();
		wk.run();
	}
	
	/*
	 * 程序入口
	 */
	public void run() {
		
		//读取配置文件
		readInputFile();
		
		//1-初始卸载策略
		initOffloadingResult();
		
		//2-资源调整过程
		searchAndAdjust();
		
		//3-DVFS调节
		dvfs();
		
		//4-终端发送功率控制
		powerControl();
	}
	
	/*
	 * 1-初始卸载策略
	 */
	public void initOffloadingResult() {
		//所用用户的完成时间
		
	}
	
	/*
	 * 2-资源调整过程
	 */
	public void searchAndAdjust() {
		
		//获得初始时间片[0,T]，T表示所有用户中最大的完成时间
		T = obtainMaxTime();
		
		//获取计算资源占用链表compList
		obtainCompList();
		
		//获取网络资源占用链表netList
		obtainNetList();
		
		CompListNode t_pc;	//计算资源冲突的关键时间点
		NetListNode t_pe;	//网络资源冲突的关键时间点
		
		while(true) {
			
			t_pc = null;
			t_pe = null;
			
			//搜索计算资源冲突的第一个关键点
			t_pc = searchFirstCCP();
			
			//搜索网络资源冲突的第一个关键点
			t_pe = searchFirstNCP();
			
			if (t_pc.start_time < t_pe.start_time) {	//调整计算资源冲突
				adjustCCP(t_pc);
			} else {	//调整网络资源冲突
				adjustNCP(t_pe);
			}
			
			//t_ccp = t_ncp = null，表示计算资源和网络资源都不存在冲突
			if (t_pc == null && t_pe == null) {
				break;
			}
		}
	}
	
	/*
	 * 3-DVFS调节（对在本地执行的组件）
	 */
	public void dvfs() {
		
	}
	
	/*
	 * 4-移动设备发送功率控制（对有数据传输到mec的组件）
	 */
	public void powerControl() {
		
	}
	
	
	//---------------------------1-初始卸载策略-start--------------------------
	
	//---------------------------1-初始卸载策略-end----------------------------
	
	
	//---------------------------2-资源调整过程-start--------------------------
	/*
	 * 获得初始时间片[0,T]，T表示所有用户中最大的完成时间
	 */
	public double obtainMaxTime() {
		double maxtime = 0;
		for (int i = 1; i <= N; i++) {
			if (user[i].completiontime > T) {
				maxtime = user[i].completiontime;
			}
		}
		return maxtime;
	}
	
	
	/*
	 * 获取计算资源占用链表compList
	 */
	public void obtainCompList() {
		compList = new CompListNode(0, T);
		compList.next = null;
		
		//遍历所有用户的所有组件，进行时间片划分
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				
				//如果组件在MEC执行才判断是否能进行时间片划分
				if (user[i].component[j].location == 1) {
					CompListNode pc = compList;
					//对每个节点遍历，判断是否可以进行时间片划分
					while(pc != null) {
						if (user[i].component[j].ST > pc.start_time &&
								user[i].component[j].ST < pc.end_time) {	//根据ST划分时间片
							
							//节点划分（时间片划分）
							timeslotPartitionComp(user[i].component[j].ST, pc.start_time, pc.end_time);
							
							//时间片划分后下次需要重新遍历compList，compList永远指向头节点
							pc = compList;
							
							break;		//跳出while循环
						} else if (user[i].component[j].FT > pc.start_time &&
								user[i].component[j].FT < pc.end_time) {	//根据FT划分时间片
							
							//节点划分（时间片划分）
							timeslotPartitionComp(user[i].component[j].FT, pc.start_time, pc.end_time);
							
							//时间片划分后下次需要重新遍历compList
							pc = compList;
							
							break;		//跳出while循环
						} else {
							pc = pc.next;
						}
					}
				}
			}
		}
	}
	
	/*
	 * 获取网络资源占用链表netList
	 */
	public void obtainNetList() {
		netList = new NetListNode(0, T);
		netList.next = null;
		
		//遍历所有用户的所有组件，进行时间片划分
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				for(int k = j; k < n; k++) {
					//存在传输的前提条件，即存在数据发送和数据接收
					if ((user[i].communication[j][k] > 0 && user[i].component[j].location == 0 && 
							user[i].component[k].location == 1) || 
						(user[i].communication[j][k] > 0 && user[i].component[j].location == 1 && 
							user[i].component[k].location == 0)) {
						//组件之间存在传输，数据处于发送或者接收状态
						
						NetListNode pe = netList;
						while(pe != null) {
							if (user[i].component[j].FT > pe.start_time &&
									user[i].component[j].FT < pe.end_time) {	//根据(j,k)中j的完成时间进行划分
								//网路资源时间片划分
								timeslotPartitionComp(user[i].component[j].FT, pe.start_time, pe.end_time);
								
								pe = netList;
								break;		//跳出while循环
							} else if (user[i].component[k].ST > pe.start_time &&
									user[i].component[k].ST < pe.end_time) {	//根据(j,k)中j的完成时间进行划分
								
								//网络资源时间片划分
								timeslotPartitionNet(user[i].component[k].ST, pe.start_time, pe.end_time);
								
								pe = netList;
								break;		//跳出while循环
							} else {
								pe = pe.next;
							}
						}
					} 
				}
			}
		}
	}
	
	/*
	 * 根据时间进行时间片划分计算资源:[0,T]----> [0,a],[a,b],[b,T]
	 */
	public void timeslotPartitionComp(double time, double start_time, double end_time) {
		//先要在compList中定位到该节点，及划分pc节点
		CompListNode pc;
		CompListNode pc_pre = compList;		//链表中pc的前驱节点
		
		//表示compList只有一个节点
		if (pc_pre.next == null) {	
			CompListNode node1 = new CompListNode(start_time, time);
			CompListNode node2 = new CompListNode(time, end_time);
			
			node1.next = node2; node2.next = null;
			compList = node1;
		}
		
		pc = pc_pre.next;
		while(pc != null) {
			if (pc.start_time == start_time) {
				CompListNode node1 = new CompListNode(start_time, time);
				CompListNode node2 = new CompListNode(time, end_time);
				
				//删除旧节点，并插入新的节点
				node1.next = node2; 
				node2.next = pc.next;
				pc_pre.next = node1;
				
				break;		//插入后结束循环
			}
			pc_pre = pc;
			pc = pc.next;
		}
	}
	
	/*
	 * 根据时间进行时间片划分网络资源:[0,T]----> [0,a],[a,b],[b,T]
	 */
	public void timeslotPartitionNet(double time, double start_time, double end_time) {
		//先要在netList中定位到该节点，及划分pe节点
		NetListNode pe;
		NetListNode pe_pre = netList;		//链表中pe的前驱节点
		
		//表示compList只有一个节点
		if (pe_pre.next == null) {	
			NetListNode node1 = new NetListNode(start_time, time);
			NetListNode node2 = new NetListNode(time, end_time);
			
			node1.next = node2; node2.next = null;
			netList = node1;
		}
		
		pe = pe_pre.next;
		while(pe != null) {
			if (pe.start_time == start_time) {
				NetListNode node1 = new NetListNode(start_time, time);
				NetListNode node2 = new NetListNode(time, end_time);
				
				//删除旧节点，并插入新的节点
				node1.next = node2; 
				node2.next = pe.next;
				pe_pre.next = node1;
				
				break;		//插入后结束循环
			}
			pe_pre = pe;
			pe = pe.next;
		}
	}
	
	
	/*
	 * 统计每个时间片中组件的个数（包括开始时间，不包括结束时间）
	 */
	public void obtainTimeslotNum() {
		
		CompListNode pc = compList;
		//获取计算资源占用链表每个时间片的number
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				if (user[i].component[j].location == 1) {
					while(pc != null) {
						if (pc.start_time >= user[i].component[j].ST 
								&& pc.end_time <= user[i].component[j].FT) {
							pc.number++;
						} else {
							pc = pc.next;
						}
					}
				}
			}
		}
		
		NetListNode  pe = netList;
		//获取网络资源占用链表每个时间片的number
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				for(int k = j; k < n; k++) {
					if ((user[i].communication[j][k] > 0 && user[i].component[j].location == 0 && 
							user[i].component[k].location == 1) || 
						(user[i].communication[j][k] > 0 && user[i].component[j].location == 1 && 
							user[i].component[k].location == 0)) {
						//组件之间存在传输，发送+接收
						
						while(pe != null) {
							if (pe.start_time >= user[i].component[j].FT &&
									pe.end_time <= user[i].component[k].ST) {
								pe.number++;
							} else {
								pe = pe.next;
							}
						}
					}
				}
			}
		}
	}
	
	/*
	 * 搜索计算资源冲突的第一个关键点
	 */
	public CompListNode searchFirstCCP() {
		//计算资源kr
		
//		double  first_tccp = -1;
		
		CompListNode pc = compList;
		while(pc != null) {
			if (pc.number > k * r) {
//				first_tccp = pc.start_time;
				return pc;
			}
			pc = pc.next;
		}
		return pc;	//没有计算资源冲突则返回-1
	}
	
	/*
	 * 搜索网络资源冲突的第一个关键点
	 */
	public NetListNode searchFirstNCP() {
		//网络资源nch
//		double first_tncp = -1;
		
		NetListNode pe = netList;
		while(pe != null) {
			if (pe.number > nch) {
//				first_tncp = pe.start_time;
				return pe;
			}
			pe = pe.next;
		}
		
		return pe;	//没有网络资源冲突则返回-1
	}
	
	/*
	 * 调整计算资源冲突
	 */
	public void adjustCCP(CompListNode t_pc) {
		//1-找到计算时间包含该时间片的所有组件
		//2-对组件进行调整，通过回报函数确定调整哪几个组件。调整组件个数: t_pc.number - k*r
		//3-调整之后更新组件的实际执行情况
		
		List<AdjustComponent > acList = new ArrayList<>();
		
		//这里回报函数的计算，还没有到具体调整那一步
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				//组件在MEC执行，且组件的执行时间包含时间片t_pc
				if(user[i].component[j].location == 1 && user[i].component[j].ST <= t_pc.start_time 
						&& user[i].component[j].FT >= t_pc.end_time) {
					//延迟执行组件
					double delay_reward = delayCCP(i,j,t_pc);
					
					//调整执行位置
					double change_reward = changeCCP(i,j,t_pc);
					
					//调整的组件实例
					AdjustComponent ac = new AdjustComponent();
					ac.i = i;
					ac.j = j;
					
					if (delay_reward <= change_reward) {	//延迟执行回报优，则延迟执行
						ac.way = 0;
						ac.reward = delay_reward;
					} else {	//否则改变执行位置
						ac.way = 1;
						ac.reward = change_reward;
					}
					
					acList.add(ac);
				}
			}
		}
		
		//对acList进行降序排序，选择前(t_pc.number - k*r)个组件进行调整
		sortDescByReward(acList);
		
		//更新所有组件的执行位置。acList.size() = t_pc.number
		int adjust_num = t_pc.number - k*r;
		for(int k = 0; k < acList.size(); k++) {
			
			//调整adjust_num个组件，终止调整
			if (k == adjust_num) {
				break;
			}
		}
		
		//调整过程结束和清空acList
		acList = null;
	}
	
	/*
	 * 计算资源调整——延迟执行
	 * 		返回：延迟执行增加的能耗，越小越好
	 */
	public double delayCCP(int i, int j, CompListNode t_pc) {
		double reward = Double.MAX_VALUE;
		double delay_time = 0;
		
		CompListNode pc = compList;
		while(pc != null) {
			//从t_pc往后延迟，并且第一个资源不超过资源约束条件，同时还要满足组件的最迟完成时间
			if (pc.start_time >= t_pc.start_time && pc.number < k*r 
					&& pc.start_time <= user[i].component[j].LST) {
				delay_time = pc.start_time - user[i].component[j].ST;	//延迟的时间
			}
			pc = pc.next;
		}
		
		reward = delay_time * 0;	//静态功耗。
		
		return reward;
	}
	
	/*
	 * 计算资源调整——改变执行位置。调整方式：从MEC返回到本地
	 */
	public double changeCCP(int i, int j, CompListNode t_pc) {
		double reward = 0;			//改变执行位置后增加的能耗
		double changeSendE = 0;			//增加的数据发送能耗
		double changeRecvE = 0;			//增加的数据接收能耗
		double changeDynamicE = 0;		//增加的动态能耗
		double changeStaticE  = 0;		//增加的静态能耗
		
		CompListNode pc = compList;
		while(pc != null) {
			
			pc = pc.next;
		}
		
		reward = changeSendE + changeRecvE + changeDynamicE + changeStaticE;
		return reward;
	}
	
	/*
	 * 对要调整组件的List进行降序排序，以便选取前几个进行调整
	 */
	public void sortDescByReward(List<AdjustComponent> acList) {
		Comparator<AdjustComponent> comparator = new MyComparator();
		Collections.sort(acList, comparator);
	}
	
	/*
	 * 自定义比较器
	 */
	public class MyComparator implements Comparator<AdjustComponent> {
		@Override
		public int compare(AdjustComponent o1, AdjustComponent o2) {
			if (o1.reward < o2.reward) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	/*
	 * 调整网络资源冲突
	 */
	public void adjustNCP(NetListNode t_pe) {
		//1-找到计算时间包含该时间片的所有组件
				//2-对组件进行调整，通过回报函数确定调整哪几个组件。调整组件个数: t_pc.number - k*r
				//3-调整之后更新组件的实际执行情况
				
				List<AdjustComponent > acList = new ArrayList<>();
				
				for(int i = 1; i <= N; i++) {
					for(int j = 0; j < n; j++) {
						//组件有数据发送到MEC，且组件的执行时间包含时间片t_pc
						if(true) {
							//延迟执行组件
							double delay_reward = delayNCP();
							
							//调整执行位置
							double change_reward = changeNCP();
							
							//调整的组件实例
							AdjustComponent ac = new AdjustComponent();
							ac.i = i;
							ac.j = j;
							
							if (delay_reward <= change_reward) {	//延迟执行汇报优，则延迟执行
								ac.way = 0;
								ac.reward = delay_reward;
							} else {	//否则改变执行位置
								ac.way = 1;
								ac.reward = change_reward;
							}
							
							acList.add(ac);
						}
					}
				}
				
				//对acList进行降序排序，选择前(t_pc.number - k*r)个组件进行调整
				//对acList进行降序排序，选择前(t_pc.number - k*r)个组件进行调整
				sortDescByReward(acList);
				
				//更新所有组件的执行位置。acList.size() = t_pc.number
				int adjust_num = t_pe.number - k*r;
				for(int k = 0; k < acList.size(); k++) {
					
					//调整adjust_num个组件，终止调整
					if (k == adjust_num) {
						break;
					}
				}
				
				//更新所有组件的执行位置
	}
	
	/*
	 * 网络资源调整——延迟执行
	 */
	public double delayNCP() { 
		double reward = 0;
		
		return reward;
	}
	
	/*
	 * 网络资源调整——改变执行位置
	 */
	public double changeNCP() {
		double reward = 0;
		
		return reward;
	}
	
	//---------------------------2-资源调整过程-end--------------------------
	
	
	//---------------------------3-DVFS调节-start--------------------------
	
	//---------------------------3-DVFS调节-end----------------------------
	
	
	//---------------------------4-终端发送功率控制-start--------------------------
	
	//---------------------------4-终端发送功率控制-end----------------------------
	
	/*
	 * 读取配置文件
	 */
	public void readInputFile() {
		try {
			for (int i = 1; i <= N; i++) {

				user[i] = new User();

				URL dir = WK.class.getResource(""); // 
				// 用户i的配置文件
				String filePath = dir.toString().substring(5) + "User" + i + ".txt";
				File file = new File(filePath);
				if (file.exists() && file.isFile()) {
					InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
					@SuppressWarnings("resource")
					BufferedReader br = new BufferedReader(isr);
					String line = null;

					for (int j = 0; j < n; j++) {
						// 用户i的组件之间的传输时间
						if ((line = br.readLine()) != null) {
							// System.out.println(line);
							String[] strs = line.split("\t");
							for (int k = 0; k < n; k++) {
								// 用户i的组件之间的传输时间
								user[i].communication[j][k] = Double.valueOf(strs[k]);
								System.out.print(user[i].communication[j][k] +" ");
							}
						}
						System.out.println();
					}

					line = br.readLine();
					
					// 用户i的组件在本地的执行时间
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < n; j++) {
							// 用户i的组件之间的传输时间
							user[i].component[j].exetime_mobile = Double.valueOf(strs[j]);
							System.out.print(user[i].component[j].exetime_mobile +" ");
						}
					}
					System.out.println();

					// 用户i的组件在云端执行的时间
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < n; j++) {
							user[i].component[j].exetime_mec = Double.valueOf(strs[j]);
							System.out.print(user[i].component[j].exetime_mec+ " ");
						}
					}
					line = br.readLine();
					
					// 魅族metal的不同功耗
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < 20; j++) {
							if (Double.valueOf(strs[j]) == 0) {
								break;
							}
							user[i].powerList.add(Double.valueOf(strs[j]));
						}
					}
					user[i].Pmax = user[i].powerList.get(0);
					System.out.println();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 用户（移动设备）
	 */
	class User {
		Component[] component; 		// 用户的组件集合

		double[][] communication; 	// 组件之间传输时间
		
		double completiontime;	//应用i的实际完成时间
		
		double deadline;		//应用i的截止时间
		double totalpower;		//移动设备i的总能耗
		
		double Pmax = 0;	//最大功率
		double fmax = 0; 	//最大频率
		
		double Precv;		//接收功率
		
		ArrayList<Double> powerList;		//不同频率的集合
		
		ArrayList<Double> sendPowerList;	//不同发送功率的集合
		
		public User() {
			component = new Component[n];
			for (int i = 0; i < n; i++) {
				component[i] = new Component();
			}
			communication = new double[n][n];
		}
	}
	
	/*
	 * 组件类
	 */
	class Component {
		double ST;		//组件开始时间
		double LST;		//组件最迟开始时间（传输功率控制时满足尾节点的最迟开始时间）
		
		double FT;		//组件完成时间	
		double LFT;		//组件最迟完成时间（DVFS时在mobile执行的组件满足的最迟完成时间）
		
		int location;			//组件的执行位置。0-本地，1-MEC
		
		double exetime_mobile;	//组件在mobile的执行时间
		double exetime_mec;		//组件在MEC的执行时间
		
		public Component() {
			location = -1;
			ST = 0;
			FT = 0;
		}
		
		public void clear() {
			location = -1;
			ST = 0;
			FT = 0;
		}
	}
	
	/*
	 * 计算资源占用链表
	 */
	class CompListNode {
		double start_time;		//时间片开始时间
		double end_time;		//时间片结束时间
		int number;				//处于该时间片的组件个数
		
		CompListNode next;		//计算资源链表的下一个节点
		
		public CompListNode() {
		}
		
		public CompListNode(double start_time, double end_time) {
			this.start_time = start_time;
			this.end_time = end_time;
		}
		
		@Override
		public String toString() {
			return start_time + "-" + end_time + "-" + number;
		}
	}
	
	/*
	 * 网络资源占用链表
	 */
	class NetListNode {
		double start_time;		//时间片开始时间
		double end_time;		//时间片结束时间
		int number;				//处于该时间片的组件个数
		
		NetListNode next;		//网络资源的下一个链表
		
		public NetListNode() {
		}
		
		public NetListNode(double start_time, double end_time) {
			this.start_time = start_time;
			this.end_time = end_time;
		}
		
		@Override
		public String toString() {
			return start_time + "-" + end_time + "-" + number;
		}
	}
	
	/*
	 * 要调整的组件的数据结构
	 */
	class AdjustComponent {
		int i;		//用户i
		int j;		//组件j
		
		double reward;		//回报值
		int    way;		//0-延迟执行；1-调整执行位置
	}
}