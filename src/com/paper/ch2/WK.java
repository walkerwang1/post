package com.paper.ch2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

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
		T = getMaxTime();
		
		//获取计算资源占用链表compList
		obtainCompList();
		
		//获取网络资源占用链表netList
		obtainNetList();
		
		double t_ccp = -1;	//计算资源冲突的关键时间点
		double t_ncp = -1;	//网络资源冲突的关键时间点
		
		while(true) {
			
			t_ccp = t_ncp = -1;
			
			//搜索计算资源冲突的第一个关键点
			t_ccp = searchFirstCCP();
			
			//搜索网络资源冲突的第一个关键点
			t_ncp = searchFirstNCP();
			
			if (t_ccp < t_ncp) {	//调整计算资源冲突
				
			} else {	//调整网络资源冲突
				
			}
			
			//t_ccp = t_ncp = -1，表示计算资源和网络资源都不存在冲突
			if (t_ccp == -1 && t_ncp == -1) {
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
	public double getMaxTime() {
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
							timeslotPartition(user[i].component[j].ST, pc.start_time, pc.end_time);
							
							//时间片划分后下次需要重新遍历compList，compList永远指向头节点
							pc = compList;
						} else if (user[i].component[j].FT > pc.start_time &&
								user[i].component[j].FT < pc.end_time) {	//根据FT划分时间片
							
							//节点划分（时间片划分）
							timeslotPartition(user[i].component[j].FT, pc.start_time, pc.end_time);
							
							//时间片划分后下次需要重新遍历compList
							pc = compList;
						} else {
							pc = pc.next;
						}
					}
				}
			}
		}
	}
	
	/*
	 * 根据时间进行时间片划分
	 */
	public void timeslotPartition(double time, double start_time, double end_time) {
		//先要在compList中定位到该节点
		CompListNode pc;
		CompListNode pc_pre = compList;
		
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
	 * 获取网络资源占用链表netList
	 */
	public void obtainNetList() {
		netList = new NetListNode(0, T);
		
		//遍历所有用户的所有组件，进行时间片划分
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				
			}
		}
	}
	
	/*
	 * 统计每个时间片中组件的个数（包括开始时间，不包括结束时间）
	 */
	public void obtainTimeslotNum() {
		
		CompListNode pc = compList;
		NetListNode  pe = netList;
		
		//遍历所有用户的所有组件，进行时间片划分
		for(int i = 1; i <= N; i++) {
			for(int j = 0; j < n; j++) {
				
			}
		}
	}
	
	/*
	 * 搜索计算资源冲突的第一个关键点
	 */
	public double searchFirstCCP() {
		//计算资源kr
		
		return -1;	//没有计算资源冲突则返回-1
	}
	
	/*
	 * 搜索网络资源冲突的第一个关键点
	 */
	public double searchFirstNCP() {
		//网络资源nch
		
		return -1;	//没有网络资源冲突则返回-1
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
}