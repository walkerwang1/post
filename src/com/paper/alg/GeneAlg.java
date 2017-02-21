package com.paper.alg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeneAlg {
	
	public static void main(String[] args) {
		GeneAlg geneAlg = new GeneAlg();
		geneAlg.run();
//		geneAlg.randomGenerator();
	}
	
	private static final int COMPONENTNUM = 7;	//组件个数，即染色体中的值的个数
	private static final double WEIGHT = 0.5;	//线性加权的权重
	
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
		
		//迭代一次后形成的新的染色体集合
		List<Strategy> newStrategyList = new ArrayList<>();	
		
		//开始迭代
//		int iter = 1;
//		while(iter < maxIter) {
//			
//			//计算每个卸载策略的适应度
//			
//			//计算每个策略被选中的概率
//			
//			//本代最优，与全局最优进行比较
//			
//			//比较当前最优策略和全局最优策略
//			
//			
//			select();
//			crossover(null, null);
//			mutation(null);
//		}
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
			Map<Integer, Component> c = strategy.getStructure();
			for(int ii=1; ii<=COMPONENTNUM; ii++) {
				System.out.print(c.get(ii).getExecLoc() + " ");
			}System.out.println();
			//判断随机生成的策略是否有重复
			if (!strategyList.contains(strategy)) {
				strategyList.add(strategy);
			} else {
				i--;
			}
			
		}
		
		Iterator<Strategy> iter = strategyList.iterator();
		int count = 1;
		while(iter.hasNext()) {
			Strategy s = iter.next();
			System.out.print("第" + count + "条染色体：");
			count++;
			for(int i =1; i <= COMPONENTNUM; i++) {
				System.out.print(s.getStructure().get(i).getExecLoc() + " ");
			}
			System.out.println();
		}
	}

	/**
	 * 随机生成一条染色体（一组策略）
	 */
	private Strategy randomGenerator() {
		Strategy s = new Strategy();
		Component c = new Component();
		Random rand = new Random();
		int num = 1;
		while(num <= COMPONENTNUM) {
			c.setExecLoc(Math.random() > 0.5 ? 0 : 1);	//为组件生成一个执行位置
			s.getStructure().put(num, c);
//			System.out.print("随机生成:");
			System.out.print(s.getStructure().get(num).getExecLoc() + " ");
			System.out.println();
			num++;
		}
		return s;
	}

	/**
	 * 选择运算：轮盘赌选择
	 */
	private void select() {
		
	}
	
	/**
	 * 基因交叉运算
	 */
	private Strategy crossover(Strategy s1, Strategy s2) {
		Strategy s = new Strategy();
		
		//依次比较染色体的每一个基因位，选择适应度较高的一个作为子染色体的基因位
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Map<Integer, Component> map1 = s1.getStructure();
			Map<Integer, Component> map2 = s2.getStructure();
			double fitness1 = calComponentFitness(map1.get(i));
			double fitness2 = calComponentFitness(map2.get(i));
			
			//基因位的适应度大的作为子代染色体对应的基因位
			if (fitness1 >= fitness2) {
				s.getStructure().put(i, s1.getStructure().get(i));
			} else {
				s.getStructure().put(i, s2.getStructure().get(i));
			}
		}
		return s;
	}
	
	/**
	 * 基因变异运算
	 */
	private Strategy mutation(Strategy s1) {
		Strategy s = new Strategy();
		Component maxFitnessComponent = null;
		int location = 0;	//记录适应度最大的组件的下标
		//遍历染色体的所有基因位，选择适应度大的基因
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component c = s1.getStructure().get(i);
			if(c.getFitness() > maxFitnessComponent.getFitness()) {
				maxFitnessComponent.setFitness(c.getFitness());
				location = i;
			}
		}
		//将适应度最大的基因位进行变异
		s1.getStructure().get(location).setExecLoc(1 - maxFitnessComponent.getExecLoc());
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
		double chromosomeTime = 0.0;
		double chromosomeEnergy = 0.0;
		double chromosomeFitness = 0.0;
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component component = s1.getStructure().get(i);
			
			//计算染色体的执行时间
			double componentTime = calComponentTime(component);
			
			//计算染色体的能耗
			double componentEnergy = calComponentEnergy(component);
			
			chromosomeTime += componentTime;
			chromosomeEnergy += componentEnergy;
		}
		
		//计算染色体的适应度的具体公式
		chromosomeFitness = WEIGHT * chromosomeTime + (1 - WEIGHT) * chromosomeEnergy;
		
		return chromosomeFitness;
	}
	
	/**
	 * 计算单个基因（组件）的适应度
	 */
	public double calComponentFitness(Component component) {
		return 0.0;
	}
	
	/**
	 * 计算染色体的执行时间
	 */
	public double calChromosomeTime() {
		
		return 0;
	}
	
	/**
	 * 计算染色体的能耗
	 */
	public double calChromosomeEnergy() {
		
		return 0;
	}
	
	/**
	 * 计算基因（组件）的执行时间 
	 */
	public double calComponentTime(Component component) {
		
		return 0.0;
	}
	
	/**
	 * 计算组件的能耗
	 */
	
	public double calComponentEnergy(Component component) {
		
		return 0;
	}
	
	/**
	 * 内部类:染色体结构（卸载策略）
	 * @author walkerwang
	 */
	private class Strategy {
		private Map<Integer, Component> structure;	//染色体结构，即卸载策略
		
		private double time;		//总的执行时间
		
		private double energy;		//能耗
		
		private double fitness;		//染色体适应度
		
		private double probability;		//选择的概率
		
		public Strategy() {
			structure = new HashMap<>();	//初始化structure结构，不然会有异常
		}

		public Map<Integer, Component> getStructure() {
			return structure;
		}

		public void setStructure(Map<Integer, Component> structure) {
			this.structure = structure;
		}

		public double getTime() {
			return time;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public double getEnergy() {
			return energy;
		}

		public void setEnergy(double energy) {
			this.energy = energy;
		}

		public double getFitness() {
			return fitness;
		}

		public void setFitness(double fitness) {
			this.fitness = fitness;
		}

		public double getProbability() {
			return probability;
		}

		public void setProbability(double probability) {
			this.probability = probability;
		}
	}
	
	
	/**
	 * 组件信息（染色体的基因位）
	 * @author walkerwang
	 * 
	 */
	private class Component {
		private double workload;		//工作负载
		
		private double uplinkDataSize;		//组件上传数据大小
		
		private double downloadDataSize;	//组件下载数据大小
		
		private int execLoc; 	//执行位置（本地还是云端）
		
		private double time;	//执行时间
		
		private double energy;	//能耗
		
		private double fitness;	//一个基因位的适应度
		
		private double probability;		//一个基因位被选择的概率

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

		public int getExecLoc() {
			return execLoc;
		}

		public void setExecLoc(int execLoc) {
			this.execLoc = execLoc;
		}

		public double getTime() {
			return time;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public double getEnergy() {
			return energy;
		}

		public void setEnergy(double energy) {
			this.energy = energy;
		}

		public double getFitness() {
			return fitness;
		}

		public void setFitness(double fitness) {
			this.fitness = fitness;
		}

		public double getProbability() {
			return probability;
		}

		public void setProbability(double probability) {
			this.probability = probability;
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
