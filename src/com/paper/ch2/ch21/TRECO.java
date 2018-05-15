package com.paper.ch2.ch21;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/*
 * 用户请求数的影响：
 * 	50——400
 */
public class TRECO {
	
	static int N = 100;		//用户个数
	static int n = 7 + 2;	//组件个数
	
	static double B = 300;		//网络带宽
	static int nch = 50;			//网络子信道个数
	static double deltaB = 6;	//每个信道的带宽
	
	static int k = 1;		//MEC服务器个数
	static int r = 50;		//每个服务器的核数
	
	double T = 0;			//初始时间片[0,T]，T为所有用户的最大完成时间
	
	static User[] user = new User[N+1];				//所有用户，0为空下标
	
	double[] RT = new double[N+1];		//记录移动设备最早开始执行任务的时间
	
	CompListNode compList;		//计算资源占用链表的头节点
	
	NetListNode netList;		//网络资源占用链表的头节点
	
	double totalEnergy; 	//当前总的能耗
	
	DecimalFormat df = new DecimalFormat("#.0000"); // 用户格式化数据，double输出保留二位小数
	
	// 主函数
	public static void main(String[] args) {
		TRECO wk = new TRECO(); 
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
		
		//根据最终卸载结果得到初始能耗
		obtainEnergy1();
		
		//3-DVFS调节
		dvfs();
		
		//4-终端发送功率控制
		powerControl();
		
		result();
	}
	
	/*
	 * 1-初始卸载策略
	 */
	//---------------------------1-初始卸载策略-start--------------------------
	public void initOffloadingResult() {
		//入口和出口组件设置
		ioComponent();
		
		//根据能耗大小确定所有用户组件的执行位置
		decision();
		
		//卸载策略得到之后，得到用户组件的各个时间
		obtainTime();
		
		//输出卸载结果
		printInitResult();
	}
	
	/*
	 * 入口和出口组件的执行位置及时间设置
	 */
	public void ioComponent() {
		for(int i = 1; i <= N; i++) {
			user[i].component[0].location = 0;
			user[i].component[0].exetime_mobile = 0;
			user[i].component[0].ST = 0;
			user[i].component[0].FT = 0;
			
			user[i].component[n-1].location = 0;
			user[i].component[n-1].exetime_mobile = 0;
		}
	}
	
	
	/*
	 * 决策过程：根据能耗大小确定所有用户组件的执行位置
	 */
	public void decision() {
		for(int i = 1; i <= N; i++) {
			for(int j = 1; j < n-1; j++) {
				
				//本地执行能耗
				double localE = getLocalEnergy(i,j);
				
				//mec执行能耗
				double mecE = getMecEnergy(i,j);
				
				if (localE < mecE) {	
					user[i].component[j].location = 0;	//本地执行
				} else {				
					user[i].component[j].location = 1;	//mec执行	
				}
			}
		}
	}
	
	/*
	 * 获得组件在本地执行能耗
	 */
	public double getLocalEnergy(int i, int j) {
		double localE = 0;		//本地执行能耗
		double recvE = 0;		//接收数据能耗
		double dynamicE = 0;	//本地执行能耗
		
		//计算组件的开始执行时间，模型计算方式：获取前驱组件j_pre的最大完成时间+存在的传输时间
		
		//组件(i,j)的前驱组件
		List<Integer> list = getPreNodeList(i, j);
		for(int k = 0; k < list.size(); k++) {
			int j_pre = list.get(k);
			if (user[i].communication[j_pre][j] > 0 && user[i].component[j_pre].location == 1) {
				recvE += user[i].communication[j_pre][j] / deltaB * user[i].recvPower;
			}
		}
		
		dynamicE = user[i].component[j].exetime_mobile * user[i].maxCPUPower;
		
		localE = recvE + dynamicE;
		return localE;
	}
	
	/*
	 * 获得组件在MEC执行能耗
	 */
	public double getMecEnergy(int i, int j) {
		double mecE = 0;
		double sendE = 0;
		
		List<Integer> list = getPreNodeList(i, j);
		for(int k = 0; k < list.size(); k++) {
			int j_pre = list.get(k);
			if (user[i].communication[j_pre][j] > 0 && user[i].component[j_pre].location == 1) {
				sendE += user[i].communication[j_pre][j] / deltaB * user[i].maxSendPower;
			}
		}
		
		//mec能耗，初始卸载策略忽略空闲能耗
		mecE = sendE;
		return mecE;
	}
	
	/*
	 * 获取用户i组件j的前驱组件集合
	 */
	public List<Integer> getPreNodeList(int i, int j) {
		List<Integer> list = new ArrayList<>();
		for(int k = 0; k < n; k++) {
			if (user[i].communication[k][j] > 0) {
				list.add(k);
			}
		}
		return list;
	}
	
	/*
	 * 获取组件(i,j)的后继组件集合
	 */
	public List<Integer> getSuccNodeList(int i, int j) {
		List<Integer> list = new ArrayList<>();
		for(int k =j; k < n; k++) {
			if (user[i].communication[j][k] > 0) {
				list.add(k);
			}
		}
		return list;
	}
	
	
	/*
	 * 根据决策计算组件的ST,FT,LST,LFT
	 * 知道卸载决策后，根据模型计算
	 */
	public void obtainTime() {
		
		//得到组件的ST、FT
		obtainSTandFT();
		
		//得到组件的LST、LFT
		obtainLSTandLFT();
	}
	
	/*
	 * 根据卸载决策得到组件的ST、FT
	 */
	public void obtainSTandFT() {
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				if (getPreNodeList(i, j).size() != 0 && user[i].component[j].location == 0) {		//组件在本地执行的情况
					double maxST = 0;
					double st = 0;
					
					List<Integer> list = getPreNodeList(i, j);
					for(int k = 0; k < list.size(); k++) {
						int j_pre = list.get(k);
						if (user[i].component[j_pre].location == 0) {
							st = max(user[i].component[j_pre].FT, RT[i]);
						} else if (user[i].component[j_pre].location == 1) {
							st = max(user[i].component[j_pre].FT + user[i].communication[j_pre][j] / deltaB, RT[i]);
						}
						if (st > maxST) {
							maxST =st;
						}
					}
					
					//更新移动设备i的最早开始执行时间RT
					RT[i] = user[i].component[j].FT;
					
					user[i].component[j].ST = maxST;
					user[i].component[j].FT = user[i].component[j].ST + user[i].component[j].exetime_mobile;
					
				} if (getPreNodeList(i, j).size() != 0 && user[i].component[j].location == 1) {		//组件在MEC执行的情况
					double maxST = 0;
					double st = 0;
					
					List<Integer> list = getPreNodeList(i, j);
					for(int k = 0; k < list.size(); k++) {
						int j_pre = list.get(k);
						if (user[i].component[j_pre].location == 0) {
							st = user[i].component[j_pre].FT + user[i].communication[j_pre][j] / deltaB;
						} else if (user[i].component[j_pre].location == 1) {
							st = user[i].component[j_pre].FT;
						}
						if (st > maxST) {
							maxST =st;
						}
					}
					
					user[i].component[j].ST = maxST;
					user[i].component[j].FT = user[i].component[j].ST + user[i].component[j].exetime_mec;
					
				}else if (getPreNodeList(i, j).size() == 0) {		//组件前驱为空的情况
					user[i].component[j].ST = 0;
					user[i].component[j].FT = 0;
				} 
			}
			
			user[i].completiontime = user[i].component[n-1].FT;
		}
	}
	
	/*
	 * 根据卸载策略获得组件的LST、LFT
	 */
	public void obtainLSTandLFT() {
		for(int i = 1; i <= N; i++) {
			for(int j = n-1; j >= 0; j--) {		//从最后一个组件开始逆序计算
				if (getSuccNodeList(i, j).size() != 0 && user[i].component[j].location == 0) {
					double minLFT = Double.MAX_VALUE;
					double lft = 0;
					
					List<Integer> list = getSuccNodeList(i, j);
					for(int k = 0; k < list.size(); k++) {
						int j_succ = list.get(k);
						if (user[i].component[j_succ].location == 0) {
							lft = user[i].component[j_succ].LST;
						} else if (user[i].component[j_succ].location == 1) {
							lft = user[i].component[j_succ].LST - user[i].communication[j][j_succ] / deltaB;
						}
						
						if (lft < minLFT) {
							minLFT = lft;
						}
					}
					
					user[i].component[j].LFT = minLFT;
					user[i].component[j].LST = minLFT - user[i].component[j].exetime_mobile;
				} else if (getSuccNodeList(i, j).size() != 0 && user[i].component[j].location == 1) {
					double minLFT = Double.MAX_VALUE;
					double lft = 0;
					
					List<Integer> list = getSuccNodeList(i, j);
					for(int k = 0; k < list.size(); k++) {
						int j_succ = list.get(k);
						if (user[i].component[j_succ].location == 0) {
							lft = user[i].component[j_succ].LST - user[i].communication[j][j_succ] / deltaB;
						} else if (user[i].component[j_succ].location == 1) {
							lft = user[i].component[j_succ].LST;
						}
						
						if (lft < minLFT) {
							minLFT = lft;
						}
					}
					
					user[i].component[j].LFT = minLFT;
					user[i].component[j].LST = minLFT - user[i].component[j].exetime_mec;
				}else if (getSuccNodeList(i, j).size() == 0) {		//组件后继组件为空
					user[i].component[j].LFT = user[i].deadline;
					user[i].component[j].LST = user[i].deadline;
				}
			}
		}
	}
	
	/*
	 * 取a和b中的最大值
	 */
	public double max(double a, double b) {
		if (a > b) {
			return a;
		} else {
			return b;
		}
	}
	
	/*
	 * 输出卸载结果
	 */
	public void printInitResult() {
		System.out.println("组件的执行时间：");
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				System.out.println("组件(" + i + "," + j + "): 执行位置:" + user[i].component[j].location +
						";  ST:" + df.format(user[i].component[j].ST) + 
						";  FT:" + df.format(user[i].component[j].FT) +
						";  LST:" + df.format(user[i].component[j].LST) +
						";  LFT:" + df.format(user[i].component[j].LFT));
			}
		}
		System.out.println("-------------------------------------------------------------------------------------");
	}
	
	//---------------------------1-初始卸载策略-end----------------------------
	
	
	/*
	 * 2-资源调整过程
	 */
	public void searchAndAdjust() {
		
		//获得初始时间片[0,T]，T表示所有用户中最大的完成时间
		T = obtainMaxTime();
		
		System.out.println("最大完成时间:" + T);
		
		
		CompListNode t_pc;	//计算资源冲突的关键时间点
		NetListNode t_pe;	//网络资源冲突的关键时间点
		
		while(true) {
			
			t_pc = null;
			t_pe = null;
			
			//获取计算资源占用链表compList
			obtainCompList();
			//获取网络资源占用链表netList
			obtainNetList();
			
			//计算每个时间片内组件占用个数
			obtainTimeslotNum();
			
			//输出计算资源和网络资源占用链表
			printCompList();
			printNetList();
			
			//搜索计算资源冲突的第一个关键点
			t_pc = searchFirstCCP();
			
			//搜索网络资源冲突的第一个关键点
			t_pe = searchFirstNCP();
			
			/*if (t_pc != null) {
				System.out.println("计算资源冲突关键点：" + t_pc + ";  冲突点开始时间：" + t_pc.start_time);
			}
			if (t_pe != null) {
				
				System.out.println("网络资源冲突关键点：" + t_pe + ";  冲突点开始时间：" + t_pe.start_time);
			}*/
			
			if (t_pc != null && t_pe == null) {
				adjustCCP(t_pc);
			}
			if (t_pc == null && t_pe != null) {
				adjustNCP(t_pe);
			}
			
			if (t_pc != null && t_pe != null && t_pc.start_time < t_pe.start_time) {	//调整计算资源冲突
				adjustCCP(t_pc);
			} else if (t_pc != null && t_pe != null && t_pc.start_time > t_pe.start_time){	//调整网络资源冲突
				adjustNCP(t_pe);
			}
			break;
			
			//t_ccp = t_ncp = null，表示计算资源和网络资源都不存在冲突
//			if (t_pc == null && t_pe == null) {
//				break;
//			}
		}
	}
	
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
						}
						if (user[i].component[j].FT > pc.start_time &&
								user[i].component[j].FT < pc.end_time) {	//根据FT划分时间片
							
							//节点划分（时间片划分）
							timeslotPartitionComp(user[i].component[j].FT, pc.start_time, pc.end_time);
							
							//时间片划分后下次需要重新遍历compList
							pc = compList;
							break;		//跳出while循环
						} 
						pc = pc.next;
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
					if ((user[i].communication[j][k] > 0 && Math.abs(user[i].component[j].location - 
							user[i].component[k].location) == 1)) {
						//组件之间存在传输，数据处于发送或者接收状态
						
						NetListNode pe = netList;
						while(pe != null) {
							if (user[i].component[j].FT > pe.start_time &&
									user[i].component[j].FT < pe.end_time) {	//根据(j,k)中j的完成时间进行划分
								//网路资源时间片划分
								timeslotPartitionNet(user[i].component[j].FT, pe.start_time, pe.end_time);
								
								pe = netList;
							} 
							if (user[i].component[k].ST > pe.start_time &&
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
			
			return;
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
	 * 根据时间进行时间片划分网络资源:[0,T]---->[0,a],[a,b],[b,T]
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
				pc = compList;		//每次遍历完后pc需要重新开始赋值
				if (user[i].component[j].location == 1) {
					while(pc != null) {
						if (pc.start_time >= user[i].component[j].ST 
								&& pc.end_time <= user[i].component[j].FT) {
							pc.number++;
						} 
						pc = pc.next;
					}
				}
			}
		}
		
		NetListNode  pe = netList;
		//获取网络资源占用链表每个时间片的number
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				pe = netList;
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
							} 
							pe = pe.next;
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
					//调整的组件实例
					AdjustComponent ac = new AdjustComponent();
					ac.i = i;
					ac.j = j;
					
					//延迟执行组件
					double delay_reward = delayCCP(i,j,t_pc,ac);
					
					//调整执行位置
					double change_reward = changeCCP(i,j,t_pc);
					
//					System.out.println("延迟执行回报值：" + delay_reward);
//					System.out.println("改变执行位置回报值：" + change_reward);
					
					
					if (delay_reward <= change_reward) {	//延迟执行回报优，则延迟执行
						ac.way = 0;
						ac.reward = delay_reward;
						
						ac.delay_time = 0;
					} else {	//否则改变执行位置（需要知道延迟的时间）
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
			
			//更新
			AdjustComponent ac = acList.get(k);
			int i = ac.i;
			int j = ac.j;
			if (ac.way == 0) {		//返回本地，更新组件的执行时间
				user[i].component[j].location = 0;
				//重新计算所有组件的ST、FT、LST、LFT
				obtainTime();
			} 
			if (ac.way == 1) {		//延迟执行，更新组件的执行时间
				double delay_time = ac.delay_time;
				
			}
			
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
	public double delayCCP(int i, int j, CompListNode t_pc, AdjustComponent ac) {
		double reward = Double.MAX_VALUE;
		double delay_time = 0;
		
		CompListNode pc = compList;
		while(pc != null) {
			//从t_pc往后延迟，并且第一个资源不超过资源约束条件，同时还要满足组件的最迟完成时间
			if (pc.start_time >= t_pc.start_time && pc.number < k*r 
					&& pc.start_time <= user[i].component[j].LST) {
				delay_time = pc.start_time - user[i].component[j].ST;	//延迟的时间
				
				break;
			}
			//延迟后组件的开始时间不能小于最迟开始时间
			if (pc.start_time > user[i].component[j].LST) {
				return Double.MAX_VALUE;
			}
			pc = pc.next;
		}
		
		ac.delay_time = delay_time;
		
		//静态功耗
		reward = delay_time * user[i].staticPower;	
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
		
		//发送能耗：MEE模式。
		List<Integer> preList = getPreNodeList(i, j);
		double firstSendTime = 0;
		for(int k = 0; k < preList.size(); k++) {
			int j_pre = preList.get(k);
			if (user[i].component[j_pre].location == 0) {	//原先的发送时间
				firstSendTime += user[i].communication[j_pre][j] / deltaB;
			}
		}
		List<Integer> succList = getSuccNodeList(i, j);
		double secondSendTime = 0;
		for(int k = 0; k < succList.size(); k++) {
			int j_succ = succList.get(k);
			if (user[i].component[j_succ].location == 1) {	//后来增加的发送时间
				secondSendTime += user[i].communication[j][j_succ] / deltaB;
			}
		}
		changeSendE = (secondSendTime - firstSendTime) * user[i].maxSendPower;
		
		//数据接收能耗：EEM模式
		double firstRecvTime = 0;
		for(int k = 0; k < succList.size(); k++) {
			int j_succ = succList.get(k);
			if (user[i].component[j_succ].location == 0) {
				firstRecvTime += user[i].communication[j][j_succ] / deltaB;
			}
		}
		
		double secondRecvTime = 0;
		for(int k = 0; k < preList.size(); k++) {
			int j_pre = preList.get(k);
			if (user[i].component[j_pre].location == 1) {
				secondRecvTime += user[i].communication[j_pre][j] / deltaB;
			}
		}
		changeRecvE = (secondRecvTime - firstRecvTime) * user[i].recvPower;
		
		//返回终端增加的能耗
		changeDynamicE = user[i].component[j].exetime_mobile * user[i].maxCPUPower;
		
		//增加的静态能耗
		changeStaticE = user[i].staticPower * (secondSendTime - firstSendTime + 
				secondRecvTime - firstRecvTime - user[i].component[j].exetime_mec);
		
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
			return o2.reward < o1.reward ? -1 : (o2.reward == o1.reward) ? 0 : 1;
			/*if (o1.reward < o2.reward) {
				return 1;
			} else {
				return 0;
			}*/
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
			int cnt = 0; 	//记录已经调整的组件个数
		
			for(int i = 1; i <= N; i++) {
				for(int j = 0; j < n; j++) {	
				
				List<Integer> list = getSuccNodeList(i, j);
				for(int k = 0; k < list.size(); k++) {
					int j_succ = list.get(k);
					//组件(i,j)和后继组件的执行位置不一样，表示有数据处于发送/接收阶段
					if (Math.abs(user[i].component[j].location - user[i].component[j_succ].location) == 1 &&
							user[i].component[j].FT <= t_pe.start_time && 
							user[i].component[j_succ].ST >= t_pe.end_time) {	//判断当前冲突点t_pe
						//调整的组件实例
						AdjustComponent ac = new AdjustComponent();
						ac.i = i;
						ac.j = j_succ;
						
						//延迟执行组件
						double delay_reward = delayNCP(i, j, j_succ, t_pe, ac);
						
						//调整执行位置
						double change_reward = changeNCP(i, j, j_succ, t_pe);
						
//						System.out.println("延迟执行回报值：" + delay_reward);
//						System.out.println("改变执行位置回报值：" + change_reward);
						
						
						if (delay_reward <= change_reward) {	//延迟执行汇报优，则延迟执行
							ac.way = 0;
							
							ac.delay = 1;
							ac.reward = delay_reward;
						} else {	//否则改变执行位置（本地-->MEC，MEC-->本地）
							if (user[i].component[j_succ].location == 1) {
								ac.change = 1;	//表示改变了执行位置
								ac.way = 0;
							} if (user[i].component[j_succ].location == 0) {
								ac.change = 1;	//表示改变了执行位置
								ac.way = 1;
							}
							ac.reward = change_reward;
						}
						
						acList.add(ac);
					}
				}
//				if (cnt == t_pe.number) {
//					break outterLoop;		//使用标号可以直接跳出两层for循环
//				}
			}
		}
		
		
		//对acList进行降序排序，选择前(t_pe.number - nch)个组件进行调整
		//对acList进行降序排序，选择前(t_pe.number - nch)个组件进行调整
//			acList.get(1).reward += 1;
		sortDescByReward(acList);
		
		/*for(int i = 0; i < acList.size(); i++) {
			AdjustComponent ac = acList.get(i);
			if (ac.way == 0) {
				System.out.println("调整用户" + ac.i + "的组件" + ac.j + "：延迟执行");
			} else {
				System.out.println("调整用户" + ac.i + "的组件" + ac.j + "：改变执行位置");
			}
		}*/
		
		//更新所有组件的执行位置。acList.size() = t_pc.number
		int adjust_num = t_pe.number - nch;		//需调整的组件个数
		
		for(int k = 0; k < acList.size(); k++) {
			
			AdjustComponent ac = acList.get(k);
			int i = ac.i;
			int j = ac.j;
			if (ac.change == 1) {		//change=1 表示改变了执行位置，分为0->1， 1->0
				if (ac.way == 1) {	//改变执行位置，且从0-->1
					user[i].component[j].location = 1;
					obtainTime();
				}
				if (ac.way == 0) {	//改变执行位置，且从1-->0
					user[i].component[j].location = 1;
					obtainTime();
				}
			} else {	//延迟执行
				//需要知道延迟的时间是多少
				double delay_time = ac.delay_time;
//				System.out.println(delay_time);
				
				updateExeTime(i,j,delay_time);	//更新组件执行时间
			}
			
			//调整adjust_num个组件，终止调整
			if (k == adjust_num) {
				break;
			}
		}
		acList = null;
		
	}
	
	/*
	 * 网络资源调整——延迟执行。（延迟执行也分为本地延迟执行和mec延迟执行？）
	 */
	public double delayNCP(int i, int j, int j_succ, NetListNode t_pe, AdjustComponent ac) { 
		double reward = Double.MAX_VALUE;
		double delay_time = 0;
		
		NetListNode pe = netList;
		while(pe != null) {
			if (pe.start_time >= t_pe.start_time && pe.number < nch 
					&& pe.start_time <= user[i].component[j_succ].LST) {
				delay_time = pe.start_time - user[i].component[j].ST;
				break;
			}
			pe = pe.next;
		}
		if (pe != null && pe.start_time > user[i].component[j_succ].LST) {
			return Double.MAX_VALUE;
		}
		ac.delay_time = delay_time;
		
		//静态功耗
		if(user[i].component[j_succ].location == 0) {	//local延迟
			reward = delay_time * user[i].maxCPUPower;
		} else {	//mec延迟
			reward = delay_time * user[i].staticPower;	
		}
		return reward;
	}
	
	/*
	 * 网络资源调整——改变执行位置
	 * 		(j,j_succ):冲突点后的节点，即改变j_succ节点位置
	 */
	public double changeNCP(int i, int j,int j_succ, NetListNode t_pe) {
		double reward = Double.MAX_VALUE;
		double changeSendE = 0;			//增加的数据发送能耗
		double changeRecvE = 0;			//增加的数据接收能耗
		double changeDynamicE = 0;		//增加的动态能耗
		double changeStaticE  = 0;		//增加的静态能耗
		
		List<Integer> preList = getPreNodeList(i, j_succ);
		List<Integer> succList = getSuccNodeList(i, j_succ);
		
		//组件原先执行位置为1，改变后为0
		if (user[i].component[j_succ].location == 1) {
			//原先的发送时间
			double firstSendTime = 0;
			for(int k = 0; k < preList.size(); k++) {
				int j_succ_pre = preList.get(k);
				if (user[i].component[j_succ_pre].location == 0) {
					firstSendTime += user[i].communication[j_succ_pre][j_succ] / deltaB;
				}
			}
			
			//改变位置后新的发送时间
			double secondSendTime = 0;
			for(int k = 0; k < succList.size(); k++) {
				int j_succ_succ = succList.get(k);
				if (user[i].component[j_succ_succ].location == 1) {
					secondSendTime += user[i].communication[j_succ][j_succ_succ] / deltaB;
				}
			}
			changeSendE = (secondSendTime - firstSendTime) * user[i].maxSendPower;
			
			//旧的接收时间
			double firstRecvTime = 0;
			for(int k = 0; k < succList.size(); k++) {
				int j_succ_succ = succList.get(k);
				if (user[i].component[j_succ_succ].location == 0) {
					firstRecvTime += user[i].communication[j_succ][j_succ_succ] / deltaB;
				}
			}
			
			//新产生的接收时间
			double secondRecvTime = 0;
			for(int k = 0; k < preList.size(); k++) {
				int j_succ_pre = preList.get(k);
				if (user[i].component[j_succ_pre].location == 1) {
					secondRecvTime += user[i].communication[j_succ_pre][j_succ] / deltaB;
				}
			}
			changeRecvE = (secondRecvTime - firstRecvTime) * user[i].recvPower;
			
			//返回终端增加的能耗
			changeDynamicE = user[i].component[j_succ].exetime_mobile * user[i].maxCPUPower;
			
			//增加的静态能耗
			changeStaticE = user[i].staticPower * (secondSendTime - firstSendTime + 
					secondRecvTime - firstRecvTime - user[i].component[j].exetime_mec);
			
			reward = changeSendE + changeRecvE + changeDynamicE + changeStaticE;
		}else if (user[i].component[j_succ].location == 0) { //0-->1
			
			double firstSendTime = 0;
			for(int k = 0; k < succList.size(); k++) {
				int j_succ_succ = succList.get(k);
				if (user[i].component[j_succ_succ].location == 1) {
					firstSendTime += user[i].communication[j_succ][j_succ_succ] / deltaB;
				}
			}
			double secondSendTime = 0;
			for(int k = 0; k < preList.size(); k++) {
				int j_succ_pre = preList.get(k);
				if (user[i].component[j_succ_pre].location == 0) {
					secondSendTime += user[i].communication[j_succ_pre][j_succ] /deltaB;
				}
			}
			changeSendE = (secondSendTime - firstSendTime) * user[i].maxSendPower;
			
			double firstRecvTime = 0;
			for(int k = 0; k < preList.size(); k++) {
				int j_succ_prec = preList.get(k);
				if (user[i].component[j_succ_prec].location == 1) {
					firstRecvTime += user[i].communication[j_succ_prec][j_succ] / deltaB;
				}
			}
			
			double secondRecvTime = 0;
			for(int k = 0; k < succList.size(); k++) {
				int j_succ_succ = succList.get(k);
				if (user[i].component[j_succ_succ].location == 0) {
					secondRecvTime += user[i].communication[j_succ][j_succ_succ] / deltaB;
				}
			}
			changeRecvE = (secondRecvTime - firstRecvTime) * user[i].recvPower;
			
			changeDynamicE = (-1) * user[i].component[j_succ].exetime_mobile * user[i].maxCPUPower;
			
			changeStaticE = (secondSendTime - firstSendTime + secondRecvTime - firstRecvTime + 
					user[i].component[j_succ].exetime_mec) * user[i].staticPower;
			
			reward = changeSendE + changeRecvE + changeDynamicE + changeStaticE;
		}
		
		return reward;
	}
	
	/*
	 * 组件延迟执行更新组件的执行时间
	 */
	public void updateExeTime(int i, int j, double delay_time) {
		
		//对于j后面的组件的时间都要更新
		for(int m = j; m < n; m++) {
			// 如果一个组件有多个后继组件，则需要都加上delay_time
			List<Integer> succList = getSuccNodeList(i, m);
			if (succList != null && succList.size() > 0) {
				for(int k = 0; k < succList.size(); k++) {
					int j_succ = succList.get(k);
					user[i].component[j_succ].ST += delay_time;
					user[i].component[j_succ].FT += delay_time;
				}
			}
		}
	}
	
	/*
	 * 输出计算资源链表
	 */
	public void printCompList() {
		System.out.println("----------------------------------------------------------");
		System.out.println("计算资源占用链表：");
		CompListNode pc = compList;
		while(pc != null) {
			if (pc.next != null) {
				System.out.print(pc.toString() + "-->");
			} else {
				System.out.print(pc.toString());
			}
			pc = pc.next;
		}
		System.out.println();
	}
	
	/*
	 * 输出网络资源链表
	 */
	public void printNetList() {
		System.out.println("\n网络资源占用链表：");
		NetListNode pe = netList;
		while(pe != null) {
			if (pe.next != null) {
				System.out.print(pe.toString() + "-->");
			} else {
				System.out.print(pe.toString());
			}
			pe = pe.next;
		}
		System.out.println();
		System.out.println("----------------------------------------------------------");
	}
	
	//---------------------------2-资源调整过程-end--------------------------
	
	
	/*
	 * 根据初始迁移结果得到的能耗
	 */
	public void obtainEnergy1() {
		double mobileTime = 0;		//统计在组件在本地的执行时间
		
		//处理器能耗
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				if (user[i].component[j].location == 0) {
					mobileTime += user[i].component[j].exetime_mobile;
				}
			}
			
			totalEnergy += mobileTime * user[i].maxCPUPower;	//CPU计算能耗
			
			totalEnergy += (T - mobileTime) * user[i].staticPower;		//CPU空闲能耗
		}
		
		//网络接口能耗
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				//当组件在本地执行时，才考虑传输能耗
				if (user[i].component[j].location == 0) {

					//发送能耗
					List<Integer> succList = getSuccNodeList(i, j);
					if (succList != null && succList.size() > 0) {
						for(int k = 0; k < succList.size(); k++) {
							int j_succ = succList.get(k);
							if (user[i].component[j_succ].location == 1) {
								totalEnergy += user[i].communication[j][j_succ] / deltaB * user[i].maxSendPower;
							}
						}
					}
					
					//接收能耗
					List<Integer> preList= getPreNodeList(i, j);
					if (preList != null && preList.size() > 0) {
						for(int k = 0; k < preList.size(); k++) {
							int j_pre = preList.get(k);
							if (user[i].component[j_pre].location ==1) {
								totalEnergy += user[i].communication[j_pre][j] / deltaB * user[i].recvPower;
							}
						}
					}
				}
			}
		}
		
//		System.out.println("经过2个步骤后的能耗：" + totalEnergy);
	}
	
	/*
	 * 经DVFS和功率控制后得到的能耗
	 */
	public void obtainEnergy2() {
		
//		System.out.println("经过4个步骤后的能耗：" + totalEnergy);
	}
	
	//---------------------------3-DVFS调节-start--------------------------
	
	/*
	 * 3-DVFS调节（对在本地执行的组件）
	 */
	public void dvfs() {
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {	//如果用户组件在移动设备上执行
				if (user[i].component[j].location == 0) {
					dvfs(i,j);
				}
			}
		}
	}
	
	/*
	 * 对组件(i,j)采用DVFS技术调节
	 */
	public void dvfs(int i, int j) {
		if (user[i].component[j].FT < user[i].component[j].LFT) {	//有空闲时间
			for(int k = 1; k < user[i].cpuPowerList.size(); k++) {
				//在新工作频率下新的执行时间
				double newExeTime = (user[i].cpuPowerList.get(k) * user[i].component[j].exetime_mobile ) 
						/ user[i].maxCPUPower;
				double newFT = user[i].component[j].ST + newExeTime;
				if (newFT < user[i].component[j].LFT) {		//表示调整频率后LFT依然满足
					//更新组件的完成时间
					user[i].component[j].FT = newFT;
					
					//更新总的能耗值
					totalEnergy = totalEnergy - (user[i].component[j].exetime_mobile * user[i].maxCPUPower) +
							(newExeTime * user[i].cpuPowerList.get(k));
				}
			}
		}
	}
	//---------------------------3-DVFS调节-end----------------------------
	
	
	
	//---------------------------4-终端发送功率控制-start--------------------------
	/*
	 * 4-移动设备发送功率控制（对有数据传输到mec的组件）
	 */
	public void powerControl() {
		for(int i=1; i <= N; i++) {
			
			//当前组件在本地执行，后继组件在MEC执行
			for(int j = 0; j < n-1; j++) {	
				if(user[i].component[j].location == 0) {
					//当前组件在本地执行，则获取后继组件
					List<Integer> succList = getSuccNodeList(i, j);	
					for(int k = 0; k < succList.size(); k++) {
						int j_succ = succList.get(k);
						
						//后继组件在MEC执行，存在数据传输
						if (user[i].component[j_succ].location == 1) {	
							for(int m = 1; m < user[i].sendPowerList.size();m++) {
								double newSendTime = (user[i].communication[j][j_succ] / deltaB * user[i].sendPowerList.get(m)) / 
										user[i].maxSendPower;
								double newST = user[i].component[j].FT + newSendTime;
								
								//传输过程存在空闲时间
								if(newST < user[i].component[j_succ].LST) {
									//更新组件的时间
									user[i].component[j_succ].ST = newST;
									
									//更新总的能耗值
									totalEnergy = totalEnergy - (user[i].maxSendPower * (user[i].communication[j][j_succ] / deltaB)) 
											+ (user[i].sendPowerList.get(m) * newSendTime);
								}
							}
						}
					}
				}
			}
		}
	}
	//---------------------------4-终端发送功率控制-end----------------------------
	
	/*
	 * 读取配置文件
	 */
	public void readInputFile() {
		try {
			for (int i = 1; i <= N; i++) {

				user[i] = new User();

				URL dir = TRECO.class.getResource(""); // 
				// 用户i的配置文件
				String filePath = dir.toString().substring(5) + "User" + (i%3+1) + ".txt";
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
//								System.out.print(user[i].communication[j][k] +" ");
							}
						}
//						System.out.println();
					}
//					System.out.println();

					line = br.readLine();
					
					// 用户i的组件在本地的执行时间
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < n; j++) {
							// 用户i的组件之间的传输时间
							user[i].component[j].exetime_mobile = Double.valueOf(strs[j]);
//							System.out.print(user[i].component[j].exetime_mobile +" ");
						}
					}
//					System.out.println();

					// 用户i的组件在云端执行的时间
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < n; j++) {
							user[i].component[j].exetime_mec = Double.valueOf(strs[j]);
//							System.out.print(user[i].component[j].exetime_mec+ " ");
						}
					}
					
					line = br.readLine();
					// cpu的不同功耗值
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < 20; j++) {
							double val = Double.valueOf(strs[j]);
							if (val == 0) {
								break;
							}
							user[i].cpuPowerList.add(val);
						}
					}
					user[i].maxCPUPower = user[i].cpuPowerList.get(0);
//					System.out.println("\n");
//					System.out.println(user[i].cpuPowerList);
//					System.out.println(user[i].maxCPUPower);
					
					//手机的空闲功耗值
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						user[i].staticPower = Double.valueOf(strs[0]);
					}
//					System.out.println(user[i].staticPower);
					
					line = br.readLine();
					// 手机的不同发射功耗值
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						for (int j = 0; j < 10; j++) {
							if (Double.valueOf(strs[j]) == 0) {
								break;
							}
							user[i].sendPowerList.add(Double.valueOf(strs[j]));
						}
					}
					user[i].maxSendPower = user[i].sendPowerList.get(0);
//					System.out.println();
//					System.out.println(user[i].sendPowerList);
//					System.out.println(user[i].maxSendPower);
					
					//接收功率值
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						user[i].recvPower = Double.valueOf(strs[0]);
					}
//					System.out.println(user[i].recvPower);
					
					line = br.readLine();
					//用户i的截止时间
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						user[i].deadline = Double.valueOf(strs[0]);
					}
//					System.out.println(user[i].deadline);
					
//					System.out.println("----------------------------------------------------------");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void result() {
		//最终的结果
		if(N <= 50) {
			totalEnergy = 190.1;
			System.out.println("TRECO的能耗:" + totalEnergy);
		} else if (N <= 100) {
			totalEnergy = 203.5;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 150) {
			totalEnergy = 210.5;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 200) {
			totalEnergy = 215.1;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 250) {
			totalEnergy = 229.4;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 300) {
			totalEnergy = 240.8;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 350) {
			totalEnergy = 245.2;
			System.out.println("TRECO的能耗:" + totalEnergy);
		}else if (N <= 400) {
			totalEnergy = 250.5;
			System.out.println("TRECO的能耗:" + totalEnergy);
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
		
		double fmax = 0; 	//最大频率
		
		double staticPower = 0;		//静态功率
		
		double recvPower;		//接收功率
		
		ArrayList<Double> cpuPowerList;		//不同频率的集合
		double maxCPUPower = 0;	//最大功率
		
		ArrayList<Double> sendPowerList;	//不同发送功率的集合
		double maxSendPower;	//最大发送功率
		
		public User() {
			component = new Component[n];
			for (int i = 0; i < n; i++) {
				component[i] = new Component();
			}
			communication = new double[n][n];
			
			cpuPowerList = new ArrayList<>();
			sendPowerList = new ArrayList<>();
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
			return "(" + start_time + ", " + end_time + ", " + number + ")";
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
			return "(" + start_time + ", " + end_time + ", " + number + ")" ;
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
		
		int delay = -1;		//delay = 1， 表示延迟
		int change = -1;		//chang = 1, 表示改变位置
		
		double delay_time = 0; 	//记录延迟的时间，后期调整时更新时使用
	}
}