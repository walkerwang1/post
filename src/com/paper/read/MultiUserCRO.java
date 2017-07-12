package com.paper.read;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MultiUserCRO {
	
	int COMPENENT_NUM = 7 + 2;
	
	double comm[][] = new double[COMPENENT_NUM][COMPENENT_NUM];
	
	class User {
		
		//用户请求相关参数，用户类
		double CPU_COMP_POWER;
		double CPU_IDLE_POWER;
		double NETWORK_SNED_POWER;
		double NETWORK_RECV_POWER;
		double NETWORK_IDLE_POWER;
		
		double totalCPUPower;
		double totalNetworkPower;
		double totalPower;
		double compTime;
		double transTime;
		double makespan;
	}
	
	List<User> userList = new ArrayList<>();

	//CRO相关参数
	int popSize = 50;		// 种群大小
	int maxIter = 100;		// 最大迭代次数
	double KELossRate = 0;		// 能量损失率
	double moleColl = 0;		// 决策分子反应的参数
	
	//分子种群中分子（策略）的集合
	List<Strategy> strategyList = new ArrayList<>();

	public static void main(String[] args) {
		
	}
	
	/*
	 * 程序运行开始
	 */
	@SuppressWarnings("unused")
	public void run() {
		
		initStrategyPopulation(popSize);
		
		int i = 1;
		while(i <= maxIter) {
			boolean STATUS = false;  	//判断化学反应是否发生成功
			
			Strategy s1=null,s2=null;
			double t = Math.random();
			if (t > moleColl) {		//发生单分子反应
				//生成一个（或一系列）分子进行单分子反应
				if (1 > 1) {	//单分子分解反应
					STATUS = decompose();
					if (STATUS == true) {
						//将原分子从strategyList中移除，添加分解后的两个分子
					}
				} else{		//单分子碰撞反应
					ineff_coll_on_wall(s1);
					//将原分子从strateyList中移除，添加碰撞后产生的新分子
				}
			} else {		//分子间反应
				//生成一系列的多个分子对<S1,S2>
				if (1 >1 ) {	//分子合成
					STATUS = synthesis();
					if (STATUS) {
						//更新strategyList
					}
				} else {	//分之间碰撞
					STATUS = inter_ineff_coll();
					if (STATUS) {
						//更新strategyList
					}
				}
			}
			
			//找到本代中势能最小的分子结构，更新全局最小
			
			i++;
		}
	}
	
	/*
	 * 初始化popSize个分子种群
	 */
	public void initStrategyPopulation(int popSize) {
		for(int i = 1; i <= popSize; i++) {
			//随机产生一个分子
			Strategy strategy = randomGenStrategy();
			
			if (!strategyList.contains(strategy)) {
				strategyList.add(strategy);
				
				//该策略下的响应时间和能耗
				double makespan = compMakeSpan(strategy);
				double energy = compEnergy(strategy);
				double kineticEnergy = initKineticEnergy(strategy);
				strategy.setMakespan(makespan);
				strategy.setEnergy(energy);
				strategy.setKineticEnergy(kineticEnergy);
				
			} else {
				i--;
			}
		}
	}
	
	/*
	 * 随机生成一种策略
	 */
	public Strategy randomGenStrategy() {
		Strategy s = new Strategy();
		
		Map<Integer, Integer> molecule = new LinkedHashMap<>();
		//第一个组件和最后一个组件的默认执行位置为0，即本地执行
		molecule.put(0, 0);
		molecule.put(COMPENENT_NUM, 0);
		for(int i = 1; i <= COMPENENT_NUM-1; i++) {
			Random rand = new Random();
			int pos = rand.nextInt(1);
			molecule.put(i, pos);
		}
		return s;
	}
	
	/*
	 * 计算响应时间:组件执行时间+组件之间传输时间
	 */
	public double compMakeSpan(Strategy strategy) {
		double makespan = 0;
		double exeTime = compExeTime(strategy);
		double transTime = compEnergy(strategy);
		makespan = exeTime + transTime;
		return makespan;
	}
	
	/*
	 * 组件执行时间
	 */
	public double compExeTime(Strategy strategy) {
		double exeTime = 0;
		Map<Integer, Integer> molecule = strategy.getMolecule();
		for(int i = 1; i < COMPENENT_NUM-1; i++) {
			int pos = molecule.get(i);
			if (pos == 0) {
				exeTime += 0;	//comp_mobile
			} else {
				exeTime += 0;	//comp_cloud
			}
		}
		return exeTime;
	}
	
	/*
	 * 组件之间传输时间
	 */
	public double compTransTime(Strategy strategy) {
		double transTime = 0;
		Map<Integer, Integer> molecule = strategy.getMolecule();
		for(int i = 1; i <= COMPENENT_NUM-1; i++) {
			int pos = molecule.get(i);
			int prepos = molecule.get(i-1);
			if (pos == 0 && prepos == 1) {
				transTime += comm[prepos][pos] / 1;		//发送时间：数据/带宽
			} else if (pos == 1 && prepos == 0) {
				transTime += comm[prepos][pos] / 2;		//接收时间
			} else {
				transTime += 0;
			}
		}
		return transTime;
	}
	
	/*
	 * 计算终端能耗
	 */
	public double compEnergy(Strategy strategy) {
		double totalEnergy = 0;
		double cpuEnergy = compCpuEnergy(strategy);
		double netEnergy = compNetEnergy(strategy);
		totalEnergy = cpuEnergy + netEnergy;
		return totalEnergy;
	}
	
	/*
	 * cpu能耗：cpu工作能耗+cpu空闲能耗
	 */
	public double compCpuEnergy(Strategy strategy) {
		double cpuEnergy;
		double cpuWorkEnergy = compCpuWorkEnergy(strategy);
		double cpuIdleEnergy = compCpuIdleEnergy(strategy);
		cpuEnergy = cpuWorkEnergy + cpuIdleEnergy;
		return cpuEnergy;
	}
	
	/*
	 * 计算cpu处于工作状态下的能耗
	 */
	public double compCpuWorkEnergy(Strategy strategy) {
		
		return 0;
	}
	
	/*
	 * 计算cpu处于空闲状态时的能耗
	 */
	public double compCpuIdleEnergy(Strategy strategy) {
		
		return 0;
	}
	
	/*
	 * 网络接口能耗：发送功耗+接收功耗+空闲功耗
	 */
	public double compNetEnergy(Strategy strategy) {
		double netEnergy = 0;
		double netSendEnergy = compNetSendEnergy(strategy);
		double netRecvEnergy = compNetRecvEnergy(strategy);
		double netIdleEnergy = compNetIdleEnergy(strategy);
		netEnergy = netSendEnergy + netRecvEnergy + netIdleEnergy;
		return netEnergy;
	}
	
	/*
	 * 计算网络接口处于发送状态的能耗
	 */
	public double compNetSendEnergy(Strategy strategy) {
		
		return 0;
	}
	
	/*
	 * 计算网络接口处于接收状态的能耗
	 */
	public double compNetRecvEnergy(Strategy strategy) {
		
		return 0;
	}
	
	/*
	 * 计算网络接口处于空闲时的能耗
	 */
	public double compNetIdleEnergy(Strategy strategy) {
		
		return 0;
	}
	
	/*
	 * 初始化分子动能
	 */
	public double initKineticEnergy(Strategy strategy) {
		return 0;
	}

	/*
	 * 单分子碰撞
	 */
	public boolean ineff_coll_on_wall(Strategy strategy) {
		Map<Integer, Integer> molecule = strategy.getMolecule();

		return false;
	}

	/*
	 * 单分子分解
	 */
	public boolean decompose() {

		//需判断分子是否成功，成功则更新操作
		
		return false;
	}

	/*
	 * 分子间碰撞
	 */
	public boolean inter_ineff_coll() {

		return false;
	}

	/*
	 * 分子合成
	 */
	public boolean synthesis() {

		return false;
	}
	
	/*
	 * 分子种群中选择具有最小势能的分子
	 */
	public Strategy getMinStructure() {
		Strategy minStructure = new Strategy();
		
		return minStructure;
	}
	
	
	/*
	 * 适应度函数计算，即分子势能
	 */
	public void compFitness() {
		
	}
	
	/*
	 * 初始化用户参数：从配置文件读取
	 * 读取组件之间的数据传输大小；
	 * 组件在本地和云端的执行时间
	 */
	public void initParam() {
		
	}
	

	/**
	 * 分子，即迁移决策
	 *
	 */
	class Strategy {
		Map<Integer, Integer> molecule;		//分子结构
		double potentialEnergy; // 分子势能
		double kineticEnergy; // 分子动能

		double makespan; // 该卸载策略下的响应时间
		double energy; // 该卸载策略下的能耗

		public Strategy() {
			molecule = new HashMap<>();
			potentialEnergy = 0;
			kineticEnergy = 0;
			makespan = 0;
			energy = 0;
		}

		public Map<Integer, Integer> getMolecule() {
			return molecule;
		}

		public void setMolecule(Map<Integer, Integer> molecule) {
			this.molecule = molecule;
		}

		public double getPotentialEnergy() {
			return potentialEnergy;
		}

		public void setPotentialEnergy(double potentialEnergy) {
			this.potentialEnergy = potentialEnergy;
		}

		public double getKineticEnergy() {
			return kineticEnergy;
		}

		public void setKineticEnergy(double kineticEnergy) {
			this.kineticEnergy = kineticEnergy;
		}

		public double getMakespan() {
			return makespan;
		}

		public void setMakespan(double makespan) {
			this.makespan = makespan;
		}

		public double getEnergy() {
			return energy;
		}

		public void setEnergy(double energy) {
			this.energy = energy;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null){
				return false;
			}else{
				if(this.getClass() == obj.getClass()){
					Strategy s = (Strategy)obj;
					Map<Integer, Integer> thisStructure = this.getMolecule();
					Map<Integer, Integer> objStructure = s.getMolecule();
					Set<Integer> keySet = thisStructure.keySet();
					Iterator<Integer> it = keySet.iterator();
					while(it.hasNext()){
						int name = it.next();
						if(thisStructure.get(name) != objStructure.get(name)){
							return false;
						}
					}
				}
			}
			
			return true;
		}
	}
	
	/*
	 * 组件
	 */
	class Component {
		double compCloud;		//组件在云端执行时间
		double compMobile;		//组件在移动端执行时间
		double startTime;		//组件的开始执行时间
		double endTime;			//组件的结束执行时间
		int position;			//组件执行位置。0-本地，1-云端
	}
}
