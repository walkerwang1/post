package com.paper.read;

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
	
	private static final int COMPONENTNUM = 7;	//组件个数，即染色体中的值的个数
	
	private static final double WEIGHT = 0.5;	//线性加权的权重
	
	private static final double FAILURE_RECOVERY_TIME = 5;	//错误恢复时间（假设SFR阶段不出现嵌套错误）:R*
	
	private static final double DURATION_OF_DISCONNECTION = 2;		//断点续传时，网络断开的时间：G
	
	private static final double FIRST_FAILURE_TIME = 5;   
	
	private static final double MOVING_ONELOC_SPEED = 2;	//用户移动到下一个位置的速度
	
	private static final double MOVING_TWOLOC_SPEED = 5; 	//用户移动到下两个位置的速度
	
	private static final int TRAJECTORY = 30;		//轨迹走过的位置数量
	
	private static final double MIN_PAUSE_TIME = 1;		//用户在i位置的最小停留时间
		
	private static final double MAX_PASUE_TIME = 3;		//用户在i位置的最大停留时间
	
	private static final double MIN_MOVING_SPEED = 1;		//用户从i位置向i+1位置的最小移动速度
	
	private static final double MAX_MOVING_SPEED = 4;		//用户从i位置向i+1位置的最大移动速度
	
	private static final double BASESTATION_SWITCH_TIME = 2;	//基站之间切换时间
	
	Map<Integer, Component> componentMap = new HashMap<>();
	
	//本代的染色体种群
	List<Strategy> strategyList = new ArrayList<>();
	
	//迭代一次后形成的新的染色体种群
	List<Strategy> newStrategyList = new ArrayList<>();	
	
	int GenSize = 50;	//种群规模
	int maxIter = 10; 	//最大迭代次数
	double pc = 0.9;	//交叉概率
	double pm = 0.1; 	//变异概率
	
	//生成一条用户移动的轨迹
	Map<Integer, Trajectory> trajectoryMap = new HashMap<>();
	
	
	DecimalFormat df = new DecimalFormat("#.0000");	//用户格式化数据，double输出保留三位小数
	
	int currentLoc = 1;  	//记录用户当前所处位置
	
	/**
	 * 遗传算法运行入口
	 */
	public void run() {
		
		currentLoc = 1;
		//初始化每个组件的信息
		initComponentInfo(componentMap);
//		printComponentInfo(componentMap);
		
		//随机产生一组GenSize条染色体
		initStrategyPopulation(GenSize, strategyList);
//		printStrategy(strategyList);
		
		//生成种群后更新Component类信息
		updateComponentMap(strategyList, componentMap);
		
		//初始化用户轨迹
		initTrajectory(trajectoryMap);
		
		Strategy globalBestStrategy = strategyList.get(0);
		globalBestStrategy.setCurrentLoc(currentLoc);
		
		//开始迭代
		int iter = 1;
		while(iter <= maxIter) {
			//本代染色体中最优的卸载策略
			Strategy localBestStrategy = new Strategy();
			
			//计算每个卸载策略的适应度
			double allChromosomeFitness = calAllChromosomeFitness(strategyList);
			
			localBestStrategy = selectMinFitnessChromosome(strategyList);
			localBestStrategy.setCurrentLoc(currentLoc);
			
			//本代最优，与全局最优进行比较:若本代中的染色体比全局最优的染色体的适应度更小，则更新全局染色体
			if (localBestStrategy.getFitness() < globalBestStrategy.getFitness()) {
				globalBestStrategy = localBestStrategy;
			}
			
			//计算每条染色体被选中的概率和累计概率
			calChromosomeProbability(strategyList, allChromosomeFitness);
			System.out.println("第" + iter + "代染色体结果：");
			printStrategy(strategyList);
			System.out.println();
			
			//选择运算：选择出GenSize个优秀的染色体
			strategyList = select(strategyList);
			
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
			
			
			
			//变异运算:mutationNum次
			int mutationNum = (int)(strategyList.size() * pm);
			List<Strategy> mutationStrategyList = selectStrategyNum(strategyList, mutationNum);
			
			//由于交叉运算是两条父类染色体生成一条子类染色体
			int addStrategyNum = GenSize - newStrategyList.size() - mutationStrategyList.size();
			for(int i = 0; i < addStrategyNum; i++) {
				Strategy s = randomGenerator();
				if (!newStrategyList.contains(s)) {
					newStrategyList.add(s);
				} else {
					i--;
				}
			}
			
			
			
			for(int i = 0; i < mutationNum; i++) {
//				Strategy s = mutation(mutationStrategyList.get(i));
//				if (!newStrategyList.contains(s)) {
//					newStrategyList.add(s);
//				} else {
//					i--;
//				}
				newStrategyList.add(mutation(mutationStrategyList.get(i)));
			}
			
			//产生新的染色体种群newStrategyList
			strategyList = newStrategyList;
			newStrategyList = new ArrayList<>();
			
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
			int index = random.nextInt(selectNum);	//产生下标0-49
			Strategy s = strategyList.get(index);
			selectStrategyList.add(s);
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
			double accumulateProbability = calAccumulateProbability(s, i);
			s.setAccumulateProbability(Double.valueOf(df.format(accumulateProbability)));
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
			accumulateProbability += strategyList.get(i).getProbability();
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
				double accumulateProbability = s.getAccumulateProbability();
				if (probability < accumulateProbability) {	//说明概率落在s上，即s被选择中
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
		Component minFitnessComponent = s1.getStructure().get(1);
		int location = 1;	//记录适应度最小的基因的下标
		//遍历染色体的所有基因位，选择适应度小的基因
		for(int i = 2; i <= COMPONENTNUM; i++) {
			Component c = s1.getStructure().get(i);
			if(c.getFitness() < minFitnessComponent.getFitness()) {
				minFitnessComponent = c;
//				minFitnessComponent.setFitness(c.getFitness());
				location = i;
			}
		}
		//将适应度最小的基因位进行变异
		s1.getStructure().put(location, minFitnessComponent);
		s1.getStructure().get(location).setExecLoc(1 - minFitnessComponent.getExecLoc());
		return s1;
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
		
//		//计算染色体的适应度的具体公式
//		chromosomeFitness = WEIGHT * chromosomeTime + (1 - WEIGHT) * chromosomeEnergy;
//		
//		chromosomeFitness = 0;
		
		for(int i = 1; i <= COMPONENTNUM; i++) {
			chromosomeFitness += calComponentFitness(s1.getStructure().get(i), i);
		}
		
		chromosomeTime = Double.valueOf(df.format(chromosomeTime));
		chromosomeEnergy = Double.valueOf(df.format(chromosomeEnergy));
		chromosomeFitness = Double.valueOf(df.format(chromosomeFitness));
		
		strategyList.get(loc).setTime(chromosomeTime);
		strategyList.get(loc).setEnergy(chromosomeEnergy);
		strategyList.get(loc).setFitness(chromosomeFitness);
		return chromosomeFitness;
	}
	
	/**
	 * 计算单个基因（组件）的适应度
	 * @param component  组件
	 * @param i  组件下标
	 */
	public double calComponentFitness(Component component, int i) {
		double componentTime = 0;
		double componentEnergy = 0;
		double componentFitness = 0;
		double componentExeTime = calComponentEnergy(component, i);
		
		//获取用户的当前位置信息
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double pauseTime = trajectory.getPauseTime();
		double movingSpeed = trajectory.getMovingSpeed();
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDataRate = trajectory.getDownloadDataRate();
		
		//网络断开，执行容错机制
		if (uplinkDataRate == 0 || downloadDataRate == 0) {
			//选择一种恢复机制，返回使用该机制的执行时间，能耗
			double[] ret = startFaultTolerance(component, i, currentLoc);
			componentTime = ret[0];
			componentEnergy = ret[1];
		} else {
			//计算基因的执行时间
			componentTime = calComponentTime(component, i);
			
			//计算基因的能耗
			componentEnergy = calComponentEnergy(component, i);
		}
			
		//执行时间大于停留时间，用户向下一个位置移动
		if (pauseTime < componentExeTime) {
			//移动到下一个位置
			if (movingSpeed <= MOVING_ONELOC_SPEED) {
				currentLoc++;
			}
			//移动到下两个位置
			if (MOVING_ONELOC_SPEED < movingSpeed && movingSpeed <= MOVING_TWOLOC_SPEED) {
				currentLoc += 2;
			}
		}
		
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
		for (int i = 0; i < GenSize; i++) {
			Strategy s1 = strategyList.get(i);
			for (int j = 1; j <= COMPONENTNUM; j++) {
				Component c1 = s1.getStructure().get(j);
				c1.setTime(calComponentTime(c1, j));
			}
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
		for (int i = 0; i < GenSize; i++) {
			Strategy s1 = strategyList.get(i);
			for (int j = 1; j <= COMPONENTNUM; j++) {
				Component c1 = s1.getStructure().get(j);
				c1.setTime(calComponentEnergy(c1, j));
			}
		}
		return chromosomeEnergy;
	}
	
	/**
	 * 计算基因（组件）的执行时间 
	 * @param c	 组件对象
	 * @param i	 第i个组件
	 */
	public double calComponentTime(Component c, int i) {
		double componentExeTime = 0.0;
		Component component = componentMap.get(i);
		
		//获取用户的当前位置信息
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double movingSpeed = trajectory.getMovingSpeed();
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDataRate = trajectory.getDownloadDataRate();
		
		//componentMap和strategyList中的Map不互通处理一下即可
		component.setExecLoc(c.getExecLoc());	
		if (component.getExecLoc() == 0) {	//在移动端执行
			componentExeTime = component.getWorkload() / MobileDeviceInfo.cpu_speed;	//1
		} else {	//组件在云端执行
			//组件在移动端：时间=上传时间+计算时间+下载时间+等待时间
			componentExeTime = component.getUplinkDataSize() / uplinkDataRate + component.getWorkload() / CloudServerInfo.cpu_speed +
					component.getDownloadDataSize() / downloadDataRate + component.getWaitingTime();		//3	
		}
		c.setTime(componentExeTime);
		return componentExeTime;
	}
	
	/**
	 * 计算组件的能耗
	 */
	public double calComponentEnergy(Component c, int loc) {
		double componentEnergy = 0.0;
		double componentExeTime = 0.0;
		
		//获取用户的当前位置信息
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double pauseTime = trajectory.getPauseTime();
		double movingSpeed = trajectory.getMovingSpeed();
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDataRate = trajectory.getDownloadDataRate();
		
		Component component = componentMap.get(loc);
		component.setExecLoc(c.getExecLoc());
		if (component.getExecLoc() == 0) {	//在移动端执行
			componentExeTime = component.getWorkload() / MobileDeviceInfo.cpu_speed;	//1
			componentEnergy = componentExeTime * MobileDeviceInfo.compute_power;	//1
		} else {	//组件在云端执行
			componentEnergy = (component.getUplinkDataSize() / uplinkDataRate) * MobileDeviceInfo.uplink_power +
					(component.getDownloadDataSize() / downloadDataRate) * MobileDeviceInfo.download_power + 1;  	//2
		}
		c.setEnergy(componentEnergy);
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
		
		//从本地文件读取每个组件的信息：工作负载、上传数据大小、下载数据大小、等待时间
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
	 * 生成一条轨迹（如果是多用户，可以给每个用户生成一条轨迹）
	 * @param trajectoryMap
	 */
	private void initTrajectory(Map<Integer, Trajectory> trajectoryMap) {
		Random random = new Random();
		for(int i = 1; i <= TRAJECTORY; i++) {
			Trajectory trajectory = new Trajectory();
			trajectory.setLocNum(i);
			
			//在[MIN_PAUSE_TIME, MAX_PASUE_TIME]之间的随机数，表示用户在i位置的停留时间
			double pauseTime = Math.random() * (MAX_PASUE_TIME - MIN_PAUSE_TIME) + MIN_PAUSE_TIME;
			trajectory.setPauseTime(pauseTime);
			
			//在[MAX_MOVING_SPEED, MIN_MOVING_SPEED]之间的随机数，表示用户从i位置移动到i+1位置的移动速度
			double movingSpeed = Math.random() * (MAX_MOVING_SPEED - MIN_MOVING_SPEED) + MIN_MOVING_SPEED;
			trajectory.setMovingSpeed(movingSpeed);
			
			double uplinkDataRate, downloadDataRate;
			
			//表示从第1个位置B最强，移动过程中数据传输速度不断降低
			if (i % 10 == 0) {
				uplinkDataRate = 0;		//设置在位置10/20/30数据传输速率为0
			} else {
				uplinkDataRate = 4 * (10 - i);
			}
			downloadDataRate = uplinkDataRate;
			
			trajectory.setUplinkDataRate(uplinkDataRate);
			trajectory.setDownloadDataRate(downloadDataRate);
			trajectoryMap.put(i, trajectory);
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
			//对象创建（*）：每一个Component对象都要创建一个对象，不然都在内存中是指向同一个对象
			Component c = new Component();		
			
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
			System.out.print("   累计概率:" + s.getAccumulateProbability());
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
		System.out.println("------------------------------------------------");
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
	 * 启动容错机制:容错：只考虑一次就恢复成功的情况，不考虑嵌套错误恢复
	 * @param c	 出现网络断开的组件
	 * @param i	组件的位置
	 * @param currentLoc	出错组件所在位置
	 */
	private double[] startFaultTolerance(Component c,int i, int currentLoc) {
		double[] ret = new double[2];
		
		double finalExeTime = 0, finalEnergy = 0;
		
		double exeTime = calComponentTime(c, i);
		
		//发生错误重新执行组件
		double restartServiceTime = calRestartServiceTime( c, i, currentLoc, exeTime);
		double restartServiceEnergy = calRestartServiceEnergy( c, i,  currentLoc, exeTime);
		
		double fitnessRestartServcie = WEIGHT * restartServiceTime + (1 - WEIGHT) * restartServiceEnergy;
		
		//发生错误从错误点恢复继续执行
		double faultToleranceTime = calFaultToleranceTime( c, i,  currentLoc, exeTime);
		double faultToleranceEnergy = calFaultToleranceEnergy( c, i,  currentLoc, exeTime);
		
		double fitnessWithFaultTolerance = WEIGHT * faultToleranceTime + (1 - WEIGHT) * faultToleranceEnergy;
		
		//基站之间切换
		double switchBaseStationTime = calComponentTime(c, i);
		double switchBaseStationEnergy = calComponentEnergy(c, i);
		
		double fitnessSwitchBaseStation =  WEIGHT * (switchBaseStationTime + BASESTATION_SWITCH_TIME) + (1 - WEIGHT) * switchBaseStationEnergy;
		
		//发生错误的时间小于组件执行时间，则错误发生
		if (FIRST_FAILURE_TIME < exeTime) {
			if (fitnessRestartServcie < fitnessWithFaultTolerance) {
				finalExeTime = restartServiceTime;
				finalEnergy = restartServiceEnergy;
			} else {
				finalExeTime = faultToleranceTime;
				finalEnergy = faultToleranceEnergy;
			}
			
			if (fitnessWithFaultTolerance < fitnessSwitchBaseStation) {
				finalExeTime = faultToleranceTime;
				finalEnergy = faultToleranceEnergy;
			} else {
				finalExeTime = switchBaseStationTime;
				finalEnergy = switchBaseStationEnergy;
			}
		}
		
		ret[0] = finalExeTime;
		ret[1] = finalEnergy;
		
		return ret;
	}
	
	/**
	 * 重新开始执行的时间
	 */
	public double calRestartServiceTime(Component c,int i, int currentLoc, double exeTime) {
		double restartServiceTime = 0;
		if (FIRST_FAILURE_TIME >= exeTime) {
			restartServiceTime = exeTime;
		} else {
			restartServiceTime = FIRST_FAILURE_TIME + FAILURE_RECOVERY_TIME + exeTime;
		}
		return restartServiceTime;
	}
	
	/**
	 * 重新开始执行的能耗
	 */
	public double calRestartServiceEnergy(Component c,int i, int currentLoc, double exeTime) {
		double restartServiceEnergy = 0;
		double sourceEnergy = calComponentEnergy(c, i);
		
		//上传、计算、下载时三个阶段的时间
		double[] threePhraseTime = calThreePhraseTime(c, i, currentLoc);
		
		double uplinkTime = threePhraseTime[0];
		double compTime = threePhraseTime[1];
		double downloadTime  = threePhraseTime[2];
		
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDateRate = trajectory.getDownloadDataRate();
		
		//第一次发生错误的时间 >= exeTime，表示错误发生时组件已经执行完了
		if (FIRST_FAILURE_TIME >= exeTime) {
			restartServiceEnergy = sourceEnergy;
		}
		
		//上传数据阶段发生错误
		if (FIRST_FAILURE_TIME < uplinkTime) {
			//上传一半的数据时错误发生
			uplinkTime = c.getUplinkDataSize() / 2 / uplinkDataRate;
			restartServiceEnergy = FIRST_FAILURE_TIME * uplinkDataRate + sourceEnergy;
		}
		
		//在云端执行组件时发生错误
		if (FIRST_FAILURE_TIME >= uplinkTime && FIRST_FAILURE_TIME < (uplinkTime + compTime)) {
			restartServiceEnergy = uplinkTime * uplinkDataRate + sourceEnergy;
		}
		
		//下载数据阶段发生错误
		if (FIRST_FAILURE_TIME >= (uplinkTime + compTime) && FIRST_FAILURE_TIME < exeTime) {
			downloadTime = c.getDownloadDataSize() / 2 / downloadDateRate;
			restartServiceEnergy = uplinkTime * uplinkDataRate + downloadTime * downloadDateRate + sourceEnergy;
		}
		
		return restartServiceEnergy;
	}
	
	/**
	 * 从断点继续执行的时间
	 */
	public double calFaultToleranceTime(Component c,int i, int currentLoc, double exeTime) {
		double faultToleranceTime = 0;
		
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDateRate = trajectory.getDownloadDataRate();
		
		//上传、计算、下载时三个阶段的时间
		double[] threePhraseTime = calThreePhraseTime(c, i, currentLoc);
		double uplinkTime = threePhraseTime[0];
		double compTime = threePhraseTime[1];
		double downloadTime  = threePhraseTime[2];
				
		//第一次发生错误的时间 >= exeTime，表示错误发生时组件已经执行完了
		if (FIRST_FAILURE_TIME >= exeTime) {
			faultToleranceTime = exeTime;
		}
		
		//上传数据阶段发生错误
		if (FIRST_FAILURE_TIME < uplinkTime) {
			//上传一部分的数据时错误发生
			double hasUplinkDataSize = c.getUplinkDataSize() * Math.random();
			faultToleranceTime = hasUplinkDataSize / uplinkDataRate * DURATION_OF_DISCONNECTION + FAILURE_RECOVERY_TIME +
					(c.getUplinkDataSize() - hasUplinkDataSize) / uplinkDataRate + compTime + c.getWaitingTime() + downloadTime;
		}
		
		//在云端执行组件时发生错误
		if (FIRST_FAILURE_TIME >= uplinkTime && FIRST_FAILURE_TIME < (uplinkTime + compTime)) {
			faultToleranceTime = uplinkTime + compTime + c.getWaitingTime() + DURATION_OF_DISCONNECTION + 
					FAILURE_RECOVERY_TIME + downloadTime;
		}
		
		//下载数据阶段发生错误
		if (FIRST_FAILURE_TIME >= (uplinkTime + compTime) && FIRST_FAILURE_TIME < exeTime) {
			//下载一部分数据时错误发生
			double hasDownloadDataSize = c.getDownloadDataSize() * Math.random();
			faultToleranceTime = uplinkTime + compTime + c.getWaitingTime() + hasDownloadDataSize / downloadDateRate +
					DURATION_OF_DISCONNECTION + FAILURE_RECOVERY_TIME + (c.getDownloadDataSize() - hasDownloadDataSize) / downloadDateRate;
		}
		
		return faultToleranceTime;
	}
	
	/**
	 * 从断点继续执行的能耗
	 */
	public double calFaultToleranceEnergy(Component c,int i, int currentLoc, double exeTime) {
		double faultToleranceEnergy = 0;
		
		//没出错时的能耗
		double sourceEnergy = calComponentEnergy(c, i);
		
		//上传、计算、下载时三个阶段的时间
		double[] threePhraseTime = calThreePhraseTime(c, i, currentLoc);
		double uplinkTime = threePhraseTime[0];
		double compTime = threePhraseTime[1];
		double downloadTime  = threePhraseTime[2];
		
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDateRate = trajectory.getDownloadDataRate();
		
		//第一次发生错误的时间 >= exeTime，表示错误发生时组件已经执行完了
		if (FIRST_FAILURE_TIME >= exeTime) {
			faultToleranceEnergy = sourceEnergy;
		}
		
		//上传数据阶段发生错误
		if (FIRST_FAILURE_TIME < uplinkTime) {
			//上传一半的数据时错误发生
			double hasUplinkDataSize = c.getUplinkDataSize() * Math.random();
			faultToleranceEnergy = hasUplinkDataSize / uplinkDataRate * MobileDeviceInfo.uplink_power + 
					(c.getUplinkDataSize() - hasUplinkDataSize) / uplinkDataRate * MobileDeviceInfo.uplink_power + 
					c.getDownloadDataSize() / downloadDateRate * MobileDeviceInfo.download_power;
		}
		
		//在云端执行组件时发生错误
		if (FIRST_FAILURE_TIME >= uplinkTime && FIRST_FAILURE_TIME < (uplinkTime + compTime)) {
			faultToleranceEnergy = uplinkTime * MobileDeviceInfo.uplink_power + downloadTime * MobileDeviceInfo.download_power;
		}
		
		//下载数据阶段发生错误
		if (FIRST_FAILURE_TIME >= (uplinkTime + compTime) && FIRST_FAILURE_TIME < exeTime) {
			double hasDownloadDataSize = c.getDownloadDataSize() * Math.random();
			faultToleranceEnergy = uplinkTime * MobileDeviceInfo.uplink_power + hasDownloadDataSize / downloadDateRate * MobileDeviceInfo.download_power +
					(c.getDownloadDataSize() - hasDownloadDataSize) / downloadDateRate * MobileDeviceInfo.download_power;
		}
		
		return faultToleranceEnergy;
	}
	
	public double[] calThreePhraseTime(Component c,int i, int currentLoc) {
		double[] threePhraseTime = new double[3];
		
		Trajectory trajectory = trajectoryMap.get(currentLoc);
		double uplinkDataRate = trajectory.getUplinkDataRate();
		double downloadDataRate = trajectory.getDownloadDataRate();
		
		double uplinkTime = c.getUplinkDataSize() / uplinkDataRate;
		double compTime = c.getWorkload() / CloudServerInfo.cpu_speed;
		double downloadTime = c.getDownloadDataSize() / downloadDataRate;
		
		threePhraseTime[0] = uplinkTime;
		threePhraseTime[1] = compTime;
		threePhraseTime[2] = downloadTime;
		
		return threePhraseTime;
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
		
		private int currentLoc;		//该策略执行完毕用户的当前位置
		
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

		public int getCurrentLoc() {
			return currentLoc;
		}

		public void setCurrentLoc(int currentLoc) {
			this.currentLoc = currentLoc;
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
		private final static double cpu_speed = 10;		//cpu计算能力
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
	private class Trajectory {
		private int locNum;		//第i个位置
		
		private double pauseTime; 	//在该位置等待的时间
		
		private double movingSpeed;		//移动到下一个位置的速度
		
		private double uplinkDataRate; 	//数据上传速率
		
		private double downloadDataRate; 	//数据下载速率

		public Trajectory() {
		}
		
		public int getLocNum() {
			return locNum;
		}

		public void setLocNum(int locNum) {
			this.locNum = locNum;
		}

		public double getPauseTime() {
			return pauseTime;
		}

		public void setPauseTime(double pauseTime) {
			this.pauseTime = pauseTime;
		}

		public double getMovingSpeed() {
			return movingSpeed;
		}

		public void setMovingSpeed(double movingSpeed) {
			this.movingSpeed = movingSpeed;
		}

		public double getUplinkDataRate() {
			return uplinkDataRate;
		}

		public void setUplinkDataRate(double uplinkDataRate) {
			this.uplinkDataRate = uplinkDataRate;
		}

		public double getDownloadDataRate() {
			return downloadDataRate;
		}

		public void setDownloadDataRate(double downloadDataRate) {
			this.downloadDataRate = downloadDataRate;
		}
	}
}