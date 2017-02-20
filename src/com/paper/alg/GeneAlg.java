package com.paper.alg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneAlg {
	
	public static void main(String[] args) {
		GeneAlg geneAlg = new GeneAlg();
		geneAlg.run();
	}
	
	private static final int COMPONENTNUM = 7;	//组件个数，即染色体中的值的个数
	
	/**
	 * 程序主体
	 */
	public void run() {
		int GenSize = 50;	//种群规模
		int maxIter = 100; 	//最大迭代次数
		double pc = 0.9;	//交叉概率
		double pm = 0.1; 	//变异概率
		
		//用户移动的轨迹
		Map<String, String> trajectory = new HashMap<>();
		
		Map<Integer, Component> componentMap = new HashMap<>();
		
		//初始化每个组件的信息
		initComponentInfo(componentMap);
		
		//随机产生一组GenSize条染色体
		List<Strategy> strategyList = new ArrayList<>();
		initStrategyPopulation(GenSize, strategyList);
		
		//开始迭代
		int iter = 1;
		while(iter < maxIter) {
			
			//计算每个卸载策略的适应度
			
			//计算每个策略被选中的概率
			
			//本代最优，与全局最优进行比较
			
			//比较当前最优策略和全局最优策略
			
			
			select();
			crossover(null, null);
			mutation(null);
		}
	}
	
	/**
	 * 初始化每个组件的信息
	 */
	private void initComponentInfo(Map<Integer, Component> componentMap) {
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component c = new Component();
			
			double workload = 0;
			double uplinkDataSize = 0;
			double downloadDataSize = 0;
			
			//从本地文件读取每个组件的信息：工作负载、上传数据大小、下载数据大小
			String fileName = "mobileinfo.txt";
			File file = new File(fileName);
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = br.readLine();
				String[] strs = line.split(" ");
				workload = Double.valueOf(strs[0]);
				uplinkDataSize = Double.valueOf(strs[1]);
				downloadDataSize = Double.valueOf(strs[2]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			c.setWorkload(workload);
			c.setUplinkDataSize(uplinkDataSize);
			c.setDownloadDataSize(downloadDataSize);
			componentMap.put(i, c);
		}
	}
	
	/**
	 * 产生初始化种群
	 * @param genSize
	 * @param strategyList
	 */
	private void initStrategyPopulation(int genSize, List<Strategy> strategyList) {
		for(int i = 0; i < genSize; i++) {
			Strategy strategy = randomGenerator();
			
			//判断随机生成的策略是否有重复
			if (!strategyList.contains(strategy)) {
				strategyList.add(strategy);
			} else {
				i--;
			}
			
		}
	}

	/**
	 * 随机生成一条染色体（一组策略）
	 */
	private Strategy randomGenerator() {
		Strategy strategy = new Strategy();
		
		return null;
	}

	/**
	 * 选择运算
	 */
	private void select() {
		
	}
	
	/**
	 * 基因交叉运算
	 */
	private Strategy crossover(Strategy s1, Strategy s2) {
		Strategy s = new Strategy();
		
		return s;
	}
	
	/**
	 * 基因变异运算
	 */
	private Strategy mutation(Strategy s1) {
		Strategy s = null;
		
		return s;
	}
	
	/**
	 * 计算所有染色体的适应度
	 */
	private void calAllChromosomeFitness(List<Strategy> strategyList) {
		for(int i = 0; i<strategyList.size(); i++) {
			Strategy s = strategyList.get(i);
			s.fitness = calChromosomeFitness(s);
		}
	}
	
	/**
	 * 计算一条染色体的适应度
	 */
	private double calChromosomeFitness(Strategy s1) {
		
		return 0.0;
	}
	
	/**
	 * 计算单个基因的适应度
	 */
	public double calGeneFitness() {
		return 0.0;
	}
	
	/**
	 * 内部类:染色体结构（卸载策略）
	 * @author walkerwang
	 */
	private class Strategy {
		private Map<Integer, Gene> structure;	//染色体结构，即卸载策略
		private double time;		//总的执行时间
		private double energy;		//能耗
		private double fitness;		//染色体适应度
		private double probability;		//选择的概率
		
		public Strategy() {
			
		}

	}
	
	/**
	 * 基因位
	 * @author walkerwang
	 *
	 */
	private class Gene {
		private int execLoc; 	//执行位置（本地还是云端）
		private double time;	//执行时间
		private double energy;	//能耗
		private double fitness;	//一个基因位的适应度
		private double probability;		//一个基因位被选择的概率
		
		
	}
	
	/**
	 * 组件信息
	 * @author walkerwang
	 * 
	 */
	private class Component {
		private double workload;		//工作负载
		
		private double uplinkDataSize;		//组件上传数据大小
		
		private double downloadDataSize;	//组件下载数据大小

//		private double waitingTime;		//组件等待执行的时间（这个是不是有Component控制的）
		
		public double getWorkload() {
			return workload;
		}

		public void setWorkload(double workload) {
			this.workload = workload;
		}

		public double getUplinkDataSize() {
			return uplinkDataSize;
		}

		public void setUplinkDataSize(double uplinkDataSize) {
			this.uplinkDataSize = uplinkDataSize;
		}

		public double getDownloadDataSize() {
			return downloadDataSize;
		}

		public void setDownloadDataSize(double downloadDataSize) {
			this.downloadDataSize = downloadDataSize;
		}
		
	}
	
	/**
	 * 移动设备信息
	 * @author walkerwang
	 *
	 */
	private class MobileDeviceInfo {
		private final static double cpu_speed = 0;		//cpu执行速度
		private final static double compute_power = 0;	//执行任务时的能耗
		private final static double uplink_power = 0;	//上传数据时的能耗
		private final static double download_power = 0;	//下载数据时的能耗
	}
	
	/**
	 * 云端服务器信息
	 * @author walkerwang
	 *
	 */
	private class CloudServerInfo {
		private final static double cpu_speed = 0;		//cpu计算能力
	}
}
