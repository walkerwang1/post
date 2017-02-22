package com.paper.alg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class GeneAlg {
	
	public static void main(String[] args) {
		GeneAlg geneAlg = new GeneAlg();
		geneAlg.run();
	}
	
	/**
	 * 容错：只考虑一次就恢复成功的情况，不考虑嵌套错误恢复
	 */
	
	private static final int COMPONENTNUM = 7;	//组件个数，即染色体中的值的个数
	
	private static final double WEIGHT = 0.5;	//线性加权的权重
	
	private static final double FAILURE_RECOVERY_TIME = 0;	//错误恢复时间（假设SFR阶段不出现嵌套错误）
	
	private static final double DISCONNECTION_TIME = 0;		//断点续传时，网络断开的时间
	
	Map<Integer, Component> componentMap = new HashMap<>();
	
	//本代的染色体种群
	List<Strategy> strategyList = new ArrayList<>();
	
	//迭代一次后形成的新的染色体种群
	List<Strategy> newStrategyList = new ArrayList<>();	
	
	int GenSize = 50;	//种群规模
	int maxIter = 2; 	//最大迭代次数
	double pc = 0.9;	//交叉概率
	double pm = 0.1; 	//变异概率
	
	/**
	 * 遗传算法运行入口
	 */
	public void run() {
		
		//用户移动的轨迹
		Map<String, String> trajectory = new HashMap<>();
		
		//初始化每个组件的信息
		initComponentInfo(componentMap);
		printComponentInfo(componentMap);
		
		//随机产生一组GenSize条染色体
		initStrategyPopulation(GenSize, strategyList);
//		printStrategy(strategyList);
		//生成种群后更新Component类信息
		updateComponentMap(strategyList, componentMap);
		
		Strategy globalBestStrategy = strategyList.get(0);
		
		//开始迭代
		int iter = 1;
		while(iter < maxIter) {
			//本代染色体中最优的卸载策略
			Strategy localBestStrategy = new Strategy();
			
			//计算每个卸载策略的适应度
			double allChromosomeFitness = calAllChromosomeFitness(strategyList);
			
			localBestStrategy = selectMinFitnessChromosome(strategyList);
			
			
			//本代最优，与全局最优进行比较:若本代中的染色体比全局最优的染色体的适应度更小，则更新全局染色体
			if (localBestStrategy.getFitness() < globalBestStrategy.getFitness()) {
				globalBestStrategy = localBestStrategy;
			}
			
			//计算每条染色体被选中的概率和累计概率
			calChromosomeProbability(strategyList, allChromosomeFitness);
			printStrategy(strategyList);
			
			//选择运算：选择出GenSize个优秀的染色体
			strategyList = select(strategyList);
			
			/*
			//交叉运算:通过交叉概率确定交叉运算的规模
			int crossoverNum = (int)(strategyList.size() * pc);
			List<Strategy> crossoverStrategyList = selectStrategyNum(strategyList, crossoverNum);
			for(int i = 0; i < crossoverNum / 2; i++) {
				Strategy s = crossover(strategyList.get(i), strategyList.get(i + crossoverNum / 2));
				if (!newStrategyList.contains(s)) {
					newStrategyList.add(s);
				} else {
					i--;
				}
			}
			
			//由于交叉运算是两条父类染色体生成一条子类染色体，种群规模缩减到GenSize/2
			int addStrategyNum = GenSize - newStrategyList.size();
			for(int i = 0; i < addStrategyNum; i++) {
				Strategy s = randomGenerator();
				if (!newStrategyList.contains(s)) {
					newStrategyList.add(s);
				} else {
					i--;
				}
			}
			
			//变异运算:GenSize次
			int mutationNum = (int)(strategyList.size() * pm);
			List<Strategy> mutationStrategyList = selectStrategyNum(strategyList, mutationNum);
			for(int i = 0; i < mutationNum; i++) {
				Strategy s = mutation(strategyList.get(i));
				if (!newStrategyList.contains(s)) {
					newStrategyList.add(s);
				} else {
					i--;
				}
			}
			
			//产生新的染色体种群newStrategyList
			strategyList = newStrategyList;
			*/
			iter++;
		}
		
		//输出最优的染色体信息
		printChromosomeInfo(globalBestStrategy);
	}
	
	/**
	 * 选择一定数目的染色体进行交叉、变异运算
	 * @param strategyList
	 * @param selectNum
	 * @return
	 */
	private List<Strategy> selectStrategyNum(List<Strategy> strategyList, int selectNum) {
		List<Strategy> selectStrategyList = new ArrayList<>();
		for(int i = 0; i < selectNum; i++) {
			Random random = new Random();
			int index = random.nextInt(50);	//产生下标0-49
			Strategy s = strategyList.get(index);
			if (!selectStrategyList.contains(s)) {
				selectStrategyList.add(s);
			} else {
				i--;
			}
		}
		return selectStrategyList;
	}
	
	/**
	 * 选择适应度最小的染色体
	 * @param strategyList
	 * @return
	 */
	private Strategy selectMinFitnessChromosome(List<Strategy> strategyList) {
		Strategy minFitnessStrategy = strategyList.get(0);
		Iterator<Strategy> iter = strategyList.iterator();
		while(iter.hasNext()) {
			Strategy s = iter.next();
			//适应度更小，则更新minFitnessStrategy
			if (s.getFitness() < minFitnessStrategy.getFitness()) {	
				minFitnessStrategy = s;
				minFitnessStrategy.setTime(s.getTime());
				minFitnessStrategy.setEnergy(s.getEnergy());
				minFitnessStrategy.setFitness(s.getFitness());
			}
		}
		return minFitnessStrategy;
	}
	
	/**
	 * 计算染色体被选择的概率
	 * @param strategyList
	 * @param allChromosomeFitness
	 */
	private void calChromosomeProbability(List<Strategy> strategyList, double allChromosomeFitness) {
		for(int i = 0; i < strategyList.size(); i++) {
			Strategy s = strategyList.get(i);
			//每条染色体被选中的概率
			s.probability = s.fitness / allChromosomeFitness;
			
			DecimalFormat df = new DecimalFormat("#.000");
			s.probability = Double.valueOf(df.format(s.probability));
		}
		
		//计算每条染色体的累计概率
		for(int i = 0; i < strategyList.size(); i++) {
			Strategy s = strategyList.get(i);
			s.accumulateProbability = calAccumulateProbability(s, i);
			
			DecimalFormat df = new DecimalFormat("#.000");
			s.accumulateProbability = Double.valueOf(df.format(s.accumulateProbability));
		}
	}
	
	/**
	 * 染色体的累计概率
	 * @param num
	 * @return
	 */
	private double calAccumulateProbability(Strategy s, int sLoc) {
		double accumulateProbability = 0;
		int i = 0;
		while(i <= sLoc) {
			s.accumulateProbability += strategyList.get(i).getProbability();
			i++;
		}
		return accumulateProbability;
	}

	/**
	 * 选择运算：通过轮盘赌方法选择
	 */
	private List<Strategy> select(List<Strategy> strategyList) {
		List<Strategy> selectStrategyList = new ArrayList<>();
		for(int i = 0; i < GenSize; i++) {
			//产生0-1之间的随机数，轮盘赌中看其值落在哪个概率区间
			Random random = new Random();
			double probability = random.nextDouble();
			Iterator<Strategy> iter = strategyList.iterator();
			while(iter.hasNext()) {
				Strategy s = iter.next();
				if (probability < s.getAccumulateProbability()) {	//说明概率落在s上，即s被选择中
					selectStrategyList.add(s);
					break;	 //s已被选中，此次轮盘赌已经结束
				}
			}
		}
		return selectStrategyList;
	}
	
	/**
	 * 基因交叉运算：两条父类染色体交叉运算后产生一条子类染色体
	 */
	private Strategy crossover(Strategy s1, Strategy s2) {
		Strategy s = new Strategy();
		
		//依次比较染色体的每一个基因位，选择适应度较高的一个作为子染色体的基因位
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Map<Integer, Component> map1 = s1.getStructure();
			Map<Integer, Component> map2 = s2.getStructure();
			double fitness1 = calComponentFitness(map1.get(i), i);
			double fitness2 = calComponentFitness(map2.get(i), i);
			
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
	 * 基因变异运算：选择适应度最小的基因进行变异
	 */
	private Strategy mutation(Strategy s1) {
		Strategy s = new Strategy();
		Component minFitnessComponent = null;
		int location = 0;	//记录适应度最小的基因的下标
		//遍历染色体的所有基因位，选择适应度小的基因
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component c = s1.getStructure().get(i);
			if(c.getFitness() > minFitnessComponent.getFitness()) {
				minFitnessComponent.setFitness(c.getFitness());
				location = i;
			}
		}
		//将适应度最小的基因位进行变异
		s1.getStructure().get(location).setExecLoc(1 - minFitnessComponent.getExecLoc());
		return s;
	}
	
	/**
	 * 计算所有染色体的适应度之和
	 */
	private double calAllChromosomeFitness(List<Strategy> strategyList) {
		double allChromosomeFitness = 0;
		for(int i = 0; i<strategyList.size(); i++) {
			Strategy s = strategyList.get(i);
			allChromosomeFitness += calChromosomeFitness(s, i);
		}
		return allChromosomeFitness;
	}
	
	/**
	 * 计算一条染色体的适应度
	 */
	private double calChromosomeFitness(Strategy s1, int loc) {
		double chromosomeTime = 0.0;	//染色体执行时间	
		double chromosomeEnergy = 0.0;	//染色体能耗
		double chromosomeFitness = 0.0;		//染色体的适应度
		
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component component = s1.getStructure().get(i);
			
			//计算染色体的执行时间
			double componentTime = calComponentTime(component, i);
			
			//计算染色体的能耗
			double componentEnergy = calComponentEnergy(component, i);
//			double componentEnergy = 0;
			
			chromosomeTime += componentTime;
			chromosomeEnergy += componentEnergy;
		}
		
		//计算染色体的适应度的具体公式
		chromosomeFitness = WEIGHT * chromosomeTime + (1 - WEIGHT) * chromosomeEnergy;
		
		strategyList.get(loc).setTime(chromosomeTime);
		strategyList.get(loc).setEnergy(chromosomeEnergy);
		strategyList.get(loc).setFitness(chromosomeFitness);
		return chromosomeFitness;
	}
	
	/**
	 * 计算单个基因（组件）的适应度
	 */
	public double calComponentFitness(Component component, int loc) {
		double componentTime = 0;
		double componentEnergy = 0;
		double componentFitness = 0;
		
		//计算基因的执行时间
		componentTime = calComponentTime(component, loc);
		
		//计算基因的能耗
		componentEnergy = calComponentEnergy(component, loc);
		
		//计算基因的适应度的公式
		componentFitness = WEIGHT * componentTime + (1 - WEIGHT) * componentEnergy;
		
		return componentFitness;
	}
	
	/**
	 * 计算染色体的执行时间
	 */
	public double calChromosomeTime(Strategy s) {
		double chromosomeTime = 0;
		//计算每个组件的执行时间，相加即可
		Map<Integer, Component> map = s.getStructure();
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component c = map.get(i);
			chromosomeTime += calComponentTime(c, i);
		}
		return chromosomeTime;
	}
	
	/**
	 * 计算染色体的能耗
	 */
	public double calChromosomeEnergy(Strategy s) {
		double chromosomeEnergy = 0;
		//计算每个组件的执行时间，相加即可
		Map<Integer, Component> map = s.getStructure();
		for(int i = 1; i <= COMPONENTNUM; i++) {
			Component c = map.get(i);
			chromosomeEnergy += calComponentEnergy(c, i);
		}
		return chromosomeEnergy;
	}
	
	/**
	 * 计算基因（组件）的执行时间 
	 */
	public double calComponentTime(Component c, int loc) {
		double componentExeTime = 0.0;
		Component component = componentMap.get(loc);
		component.setExecLoc(c.getExecLoc());
		if (component.getExecLoc() == 0) {	//在移动端执行
			componentExeTime = component.getWorkload() / MobileDeviceInfo.cpu_speed;	//1
		} else {	//组件在云端执行
			//组件在移动端：时间=上传时间+计算时间+下载时间+等待时间
			componentExeTime = component.getUplinkDataSize() / 10 + component.getWorkload() / CloudServerInfo.cpu_speed +
					component.getDownloadDataSize() / 10 + component.getWaitingTime();		//3	
		}
		return componentExeTime;
	}
	
	/**
	 * 计算组件的能耗
	 */
	public double calComponentEnergy(Component c, int loc) {
		double componentEnergy = 0.0;
		double componentExeTime = 0.0;
		Component component = componentMap.get(loc);
		component.setExecLoc(c.getExecLoc());
		if (component.getExecLoc() == 0) {	//在移动端执行
			componentExeTime = component.getWorkload() / MobileDeviceInfo.cpu_speed;	//1
			componentEnergy = componentExeTime * MobileDeviceInfo.compute_power;	//1
		} else {	//组件在云端执行
			componentEnergy = (component.getUplinkDataSize() / 10) * MobileDeviceInfo.uplink_power +
					(component.getDownloadDataSize() / 10) * MobileDeviceInfo.download_power + 1;  	//2
		}
		return componentEnergy;
	}
	
	/**
	 * 更新strateList中的Map结构信息
	 * @param strategyList
	 * @param componentMap
	 */
	public void updateComponentMap(List<Strategy> strategyList, Map<Integer, Component> componentMap) {
		for(int i = 0; i < GenSize; i++) {
			Map<Integer, Component> map = strategyList.get(i).getStructure();
			for(int j = 1; j <= COMPONENTNUM; j++) {
				map.get(j).setWorkload(componentMap.get(j).getWorkload());
				map.get(j).setUplinkDataSize(componentMap.get(j).getUplinkDataSize());
				map.get(j).setDownloadDataSize(componentMap.get(j).getDownloadDataSize());
				map.get(j).setWaitingTime(componentMap.get(j).getWaitingTime());
			}
		}
	}
	
	/**
	 * 初始化每个组件的信息
	 */
	private void initComponentInfo(Map<Integer, Component> componentMap) {
			
		double workload = 0;
		double uplinkDataSize = 0;
		double downloadDataSize = 0;
		double waitingTime = 0;
		int count = 1;
		
		//从本地文件读取每个组件的信息：工作负载、上传数据大小、下载数据大小
		String fileName = "mobileinfo.txt";
		File file = new File(fileName);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = br.readLine()) != null) {
				Component c = new Component();
				String[] strs = line.split(" ");
				workload = Double.valueOf(strs[0]);
				uplinkDataSize = Double.valueOf(strs[1]);
				downloadDataSize = Double.valueOf(strs[2]);
				waitingTime = Double.valueOf(strs[3]);
				
				c.setWorkload(workload);
				c.setUplinkDataSize(uplinkDataSize);
				c.setDownloadDataSize(downloadDataSize);
				c.setWaitingTime(waitingTime);
				componentMap.put(count, c);
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		Strategy s = new Strategy();
		Random rand = new Random();
		Map<Integer, Component> structure = new TreeMap<>();
		int num = 1;
		while(num <= COMPONENTNUM) {
			Component c = new Component();		//重新初始化（*）
			c.setExecLoc(Math.random() > 0.5 ? 0 : 1);	//为组件生成一个执行位置
			structure.put(num, c);
			num++;
		}
//		for(int i = 1; i <= COMPONENTNUM; i++) {
//			System.out.print(structure.get(i).getExecLoc());
//		}
//		System.out.println();
		s.setStructure(structure);
		return s;
	}
	
	/**
	 * 输出染色体集合的结果
	 */
	public void printStrategy(List<Strategy> strategyList) {
		Iterator<Strategy> iter = strategyList.iterator();
		int count = 1;
		while(iter.hasNext()) {
			Strategy s = iter.next();
			System.out.print("第" + count + "条染色体：");
			for(int i = 1; i <= COMPONENTNUM; i++) {
				Component c = s.getStructure().get(i);
				System.out.print(c.getExecLoc());
			}
//			System.out.print( "   时间:" + s.getTime());
//			System.out.print( "   能耗:" + s.getEnergy());
			System.out.print("   适应度:" + s.getFitness());
			System.out.print("   选中概率:" + s.getProbability());
			System.out.println();
			count++;
		}
	}
	
	/**
	 * 输出组件信息
	 */
	public void printComponentInfo(Map<Integer, Component> componentMap) {
		System.out.println("组件的数据：负载    上传数据   下载数据   等待时间");
		for(int i = 1; i <= componentMap.size(); i++) {
			Component c = componentMap.get(i);
			System.out.print(c.getWorkload() + " " + c.getUplinkDataSize() + " " +
					c.getDownloadDataSize() + " " + c.getWaitingTime());
			System.out.println();
		}
	}
	
	/**
	 * 输出染色体信息
	 */
	public void printChromosomeInfo(Strategy globalBestStrategy) {
		System.out.print("最优染色体策略：");
		Map<Integer, Component> structureMap = globalBestStrategy.getStructure();
		for(int i = 1; i <= COMPONENTNUM; i++) {
			System.out.print(structureMap.get(i).getExecLoc());
		}
		System.out.println();
		System.out.println("时间:" + globalBestStrategy.getTime());
		System.out.println("能耗:" + globalBestStrategy.getEnergy());
		System.out.println("适应度:" + globalBestStrategy.getFitness());
	}
	
	/**
	 * 启动容错机制
	 * @param c	 出现网络断开的组件
	 */
	private void startFaultTolerance(Component c) {
		
	}
	
	/**
	 * 用户移动位置处理
	 */
	private void locationManagment() {
		//先根据用户走过的位置生成一条轨迹，每次开始卸载组件时确定用户的位置
		
		//用户首先肯定是从第一个位置开始，执行完一个组件，发送消息查询用户当前位置（即第i个位置）
		
	}
	//
	public void getLocation() {
		Random random = new Random();
		int locIndex = random.nextInt(2);
		//获得该位置的信息（主要是数据传输速率）
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
		
		private double accumulateProbability;	//累计概率（用于轮盘赌选择）
		
		public Strategy() {
			structure = new TreeMap<>();	//初始化structure结构，不然会有异常
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

		public double getAccumulateProbability() {
			return accumulateProbability;
		}

		public void setAccumulateProbability(double accumulateProbability) {
			this.accumulateProbability = accumulateProbability;
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

		private double waitingTime;		//组件等待执行的时间（这个是不是有Component控制的）
		
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

		public double getWaitingTime() {
			return waitingTime;
		}

		public void setWaitingTime(double waitingTime) {
			this.waitingTime = waitingTime;
		}
		
	}
	
	/**
	 * 移动设备信息
	 * @author walkerwang
	 *
	 */
	private class MobileDeviceInfo {
		private final static double cpu_speed = 10;		//cpu执行速度
		private final static double compute_power = 1;	//执行任务时的能耗
		private final static double uplink_power = 1;	//上传数据时的能耗
		private final static double download_power = 1;	//下载数据时的能耗
	}
	
	/**
	 * 云端服务器信息
	 * @author walkerwang
	 *
	 */
	private class CloudServerInfo {
		private final static double cpu_speed = 10;		//cpu计算能力
	}
	
	/**
	 * 用户移动轨迹
	 * @author walkerwang
	 *
	 */
	private class MovingTrajectory {
		private int locNum;		//第i个位置
		
		private double waitingTime; 	//在该位置等待的时间
		
		private double movingSpeed;		//移动到下一个位置的速度
		
		private double uplinkDataRate; 	//数据上传速率
		
		private double downloadDataRate; 	//数据下载速率
	}
}