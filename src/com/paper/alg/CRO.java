package com.paper.alg;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CRO {
	
	int NUMBER = 7;

	//CRO相关参数
	int popSize = 16;		// 种群大小
	int maxIter = 50;		// 最大迭代次数
	double KELossRate = 0.3;		// 能量损失率
	double moleColl = 0.2;		// 决策分子反应的参数
	
	double buffer = 0;
	double iKE = 0;
	double decomp, synth;	//分解和合成判断标准
	
	DecimalFormat df = new DecimalFormat("#.0000");	//用户格式化数据，double输出保留三位小数
	
	//分子种群中分子（策略）的集合
	List<Molecule> moleculeList = new ArrayList<>();

	public static void main(String[] args) {
		CRO cro = new CRO();
		cro.run();
	}
	
	/*
	 * 程序运行开始
	 */
	public void run() {
		Random rand = new Random();
		initCROParam();
		
		initStrategyPopulation(popSize);
		
		System.out.println("------------初始分子种群----------");
		printMoleculeList();
		System.out.println();
		
		int i = 1;
		while(i <= maxIter) {
			boolean STATUS = false;  	//判断化学反应是否发生成功
			double t = rand.nextDouble();
			if (t > moleColl) {		//发生单分子反应
				//选择一个分子进行单分子反应
				Molecule s = moleculeList.get(rand.nextInt(NUMBER));
				if (checkDecomp(s)) {	//单分子分解反应
					STATUS = decompose(s);
					if (!STATUS) {	//分解失败
						continue;
					}
				} else{		//单分子碰撞反应
					ineff_coll_on_wall(s);
				}
			} else {		//分子间反应
				//选择一个分子对<S1,S2>
				Molecule s1 = moleculeList.get(rand.nextInt(NUMBER));
				Molecule s2 = moleculeList.get(rand.nextInt(NUMBER));
				while (s1.equals(s2)) {
					s2 = moleculeList.get(rand.nextInt(NUMBER));
				}
				if (checkSynth(s1, s2) ) {	//分子合成
					STATUS = synthesis(s1,s2);
					if (!STATUS) {
						continue;
					}
				} else {	//分之间碰撞
					inter_ineff_coll(s1,s2);
				}
			}
			
			//找到本代中势能最小的分子结构，更新全局最小
		    Molecule minStructure = getMinStructure(moleculeList);
		    
		    Map<Integer, Integer> map = minStructure.getMolecule();
			StringBuilder sb = new StringBuilder();
			for(int j = 1; j <= NUMBER; j++) {
				sb.append(map.get(j));
			}
			System.out.println("------------------第" + i + "次迭代--------------------------");
			System.out.println("种群：" + moleculeList.size());
//			printMoleculeList();
	 		System.out.println("本代分子结构：" + sb.toString());
	 		System.out.println("本代中最小的势能：" + Double.valueOf(df.format(minStructure.PE)));

	 		minStructure.PE = Double.MAX_VALUE;
	 		i++;
		} //迭代结束
		
	}
	
	/*
	 * 输出分子集合
	 */
	public void printMoleculeList() {
		for (int i = 0; i < moleculeList.size(); i++) {
			Map<Integer, Integer> map = moleculeList.get(i).getMolecule();
			StringBuilder sb = new StringBuilder();
			for(int j = 1; j <= NUMBER; j++) {
				sb.append(map.get(j));
			}
			System.out.println(sb.toString() +":" + Double.valueOf(df.format(moleculeList.get(i).PE)));
		}
	}
	
	/*
	 * 初始化CRO的相关参数
	 */
	public void initCROParam() {
		Random rand = new Random();
		decomp = rand.nextDouble();		//单分子分解常数
		synth = rand.nextDouble();		//分子间合成常数
	}
	
	/*
	 * check单分子分解
	 */
	public boolean checkDecomp(Molecule s) {
		return s.PE >= decomp;
	}
	
	/*
	 * check分子合成
	 */
	public boolean checkSynth(Molecule s1, Molecule s2) {
		
		return (s1.KE <= synth && s2.KE <=synth);
	}
	
	/*
	 * 初始化popSize个分子种群
	 */
	public void initStrategyPopulation(int popSize) {
		double sumPE = 0.0;
		for(int i = 1; i <= popSize; i++) {
			//随机产生一个分子
			Molecule molecule = randomGenMolecule();
			
			if (!moleculeList.contains(molecule)) {
				moleculeList.add(molecule);
				//记录当前种群的最大值和最小值，然后计算适应度函数
				
				//该策略下势能PE和动能KE
				double PE = compFitness(molecule);
				molecule.setPE(PE);
				sumPE += PE;
			} else {
				i--;
			}
		}
		
		//初始化动能，[0,1]
		iKE = sumPE / popSize;
		for(int i = 0; i < moleculeList.size(); i++) {
			Molecule molecule = moleculeList.get(i);
			molecule.KE = iKE;
		}
	}
	
	/*
	 * 随机生成一种策略
	 */
	public Molecule randomGenMolecule() {
		Molecule s = new Molecule();
		
		Map<Integer, Integer> molecule = new LinkedHashMap<>();
		for(int i = 1; i <= NUMBER; i++) {
			Random rand = new Random();
			int pos = rand.nextInt(2);
			molecule.put(i, pos);
		}
		s.setMolecule(molecule);
		return s;
	}
	
	/*
	 * 产生0~1之间的书
	 */
	public int rand() {
		Random rand = new Random();
		int v = rand.nextDouble() > 0.5 ? 0 : 1;
		return v;
	}
	
	/*
	 * 初始化分子动能
	 */
	public double initKE(Molecule s) {
		s.KE = iKE;
		return s.KE;
	}

	/*
	 * 单分子碰撞
	 */
	public void ineff_coll_on_wall(Molecule s) {
		Random rand = new Random();
		Molecule newS = new Molecule();
		int point = rand.nextInt(NUMBER);
		//执行单分子碰撞操作
		for(int i = 1; i <= NUMBER; i++) {
			if (i == point) {
				newS.getMolecule().put(i, 1 - s.getMolecule().get(i));
			} else {
				newS.getMolecule().put(i, s.getMolecule().get(i));
			}
		}
		double newSPE = compFitness(newS);
		double tempBuff = s.KE + s.PE - newSPE;
		if (tempBuff >= 0) {
			newS.PE = newSPE;
			
			//产生随机数q属于[KELossRate,1]
			double q = rand.nextDouble() * (1 - KELossRate) + KELossRate;
			newS.KE = tempBuff * q;
			
			//更新buffer
			buffer = buffer + tempBuff * (1 - q);
			
			moleculeList.remove(s);
			moleculeList.add(newS);
		}
	}

	/*
	 * 单分子分解
	 */
	public boolean decompose(Molecule s) {
		boolean status = false;
		Random rand = new Random();
		
		//分解后两个新分子
		Molecule newS1 = new Molecule();
		Molecule newS2 = new Molecule();
		int point = rand.nextInt(NUMBER) + 1;	//[1,NUMBER]
		for(int i = 1; i <= NUMBER; i++) {
			if (i < point) {
				newS1.getMolecule().put(i, s.getMolecule().get(i));		//s1获得s的point之前的部分，后半部分随机生成
				newS2.getMolecule().put(i, rand.nextInt(2));
			} else {
				newS2.getMolecule().put(i, s.getMolecule().get(i));		//s2获得s的point之后的部分，前部分随机生成
				newS1.getMolecule().put(i, rand.nextInt(2));
			}
		}
		
		double newS1PE = compFitness(newS1);
		double newS2PE = compFitness(newS2);
		double tempBuff = s.KE + s.PE - newS1.PE - newS2.PE;
		if (tempBuff >= 0 ) {
			status = true;
			newS1.PE = newS1PE;
			newS2.PE = newS2PE;
			double p = rand.nextDouble();
			newS1.KE = tempBuff * p;
			newS2.KE = tempBuff * (1 - p);
			moleculeList.remove(s);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		} else if (tempBuff + buffer > 0) {
			status = true;
			newS1.PE = newS1PE;
			newS2.PE = newS2PE;
			double m1 = rand.nextDouble();
			double m2 = rand.nextDouble();
			double m3 = rand.nextDouble();
			double m4 = rand.nextDouble();
			newS1.KE = (tempBuff + buffer) * m1 * m2;
			newS2.KE = (tempBuff + buffer) * m3 * m4 * (1 - m1*m2);
			buffer = tempBuff + buffer - newS1.KE - newS2.KE;
			moleculeList.remove(s);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		} else {
			status = false;
		}
		return status;
	}

	/*
	 * 分子间碰撞
	 */
	public void inter_ineff_coll(Molecule s1, Molecule s2) {
		Random rand = new Random();
		Molecule newS1 = new Molecule();
		Molecule newS2 = new Molecule();
		
		//执行碰撞操作
		//选择两个不同的点进行分子间的碰撞操作
		int point1 = rand.nextInt(NUMBER) + 1;	 //[1,NUMBER]
		int point2 = rand.nextInt(NUMBER) + 1;
		while(point1 == point2) {
			point2 = rand.nextInt(NUMBER) + 1;
		}
		for(int i = 1; i <= NUMBER; i++) {
			if (i == point1 || i == point2) {
				newS1.getMolecule().put(i, s2.getMolecule().get(i));
				newS2.getMolecule().put(i, s1.getMolecule().get(i));
			} else {
				newS1.getMolecule().put(i, s1.getMolecule().get(i));
				newS2.getMolecule().put(i, s2.getMolecule().get(i));
			}
		}
		
		double newS1PE = compFitness(newS1);
		double newS2PE = compFitness(newS2);
		
		//能耗守恒check
		double tempBuff = s1.KE + s1.PE + s2.KE + s2.PE - newS1PE - newS2PE;
		
		//满足碰撞条件
		if (tempBuff >= 0) {
			
			double p = rand.nextDouble();
			newS1.PE = newS1PE;
			newS1.KE = tempBuff * p;
			
			newS2.PE = newS2PE;
			newS2.KE = tempBuff * (1 - p);
			
			moleculeList.remove(s1);
			moleculeList.remove(s2);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		}
	}

	/*
	 * 分子合成
	 */
	public boolean synthesis(Molecule s1, Molecule s2) {
		Random rand = new Random();
		
		Molecule newS = new Molecule();
		
		//合成操作
		for(int i = 1; i <= NUMBER; i++) {
			if (rand.nextDouble() > 0.5) {
				newS.getMolecule().put(i, s1.getMolecule().get(i));
			} else {
				newS.getMolecule().put(i, s2.getMolecule().get(i));
			}
		}
		
		double newSPE = compFitness(newS);
		
		double tempBuff = s1.KE + s1.PE + s2.KE + s2.PE - newSPE;
		if (tempBuff >= 0) {
			newS.PE = newSPE;
			newS.KE = tempBuff;
			moleculeList.remove(s1);
			moleculeList.remove(s2);
			moleculeList.add(newS);
			return true;
		}
		return false;
	}
	
	/*
	 * 分子种群中选择具有最小势能的分子
	 */
	public Molecule getMinStructure(List<Molecule> moleculeList) {
		Molecule minStructure = new Molecule();
		int minIndex = Integer.MAX_VALUE;
		double minPE = Double.MAX_VALUE;
		for(int i = 0; i < moleculeList.size(); i++) {
			Molecule m = moleculeList.get(i);
			if (m.PE < minPE) {
				minPE = m.PE;
				minIndex = i;
			}
		}
		minStructure.setMolecule(moleculeList.get(minIndex).getMolecule());
		minStructure.setKE(moleculeList.get(minIndex).getKE());
		minStructure.setPE(moleculeList.get(minIndex).getPE());
		
		return minStructure;
	}
	
	/*
	 * 适应度函数计算，即分子势能
	 */
	public double compFitness(Molecule molecule) {
		Map<Integer, Integer> m  = molecule.getMolecule();
		StringBuilder sb = new StringBuilder();
		for(int i= 1; i <= NUMBER; i++) {
			int v = m.get(i);
			sb.append(v);
		}
		int val = Integer.parseInt(sb.toString(), 2);
		
		sb = new StringBuilder("");
		for(int i = 1; i <= NUMBER; i++) {
			sb.append(1);
		}
		int max = Integer.parseInt(sb.toString(),2);
		
		return val  / Double.valueOf((max));
	}
	
	/**
	 * 分子，即迁移决策
	 *
	 */
	class Molecule {
		Map<Integer, Integer> molecule;		//分子结构
		double PE; // 分子势能
		double KE; // 分子动能

		public Molecule() {
			molecule = new HashMap<>();
			PE = 0;
			KE = 0;
		}

		public Map<Integer, Integer> getMolecule() {
			return molecule;
		}

		public void setMolecule(Map<Integer, Integer> molecule) {
			this.molecule = molecule;
		}

		public double getPE() {
			return PE;
		}

		public void setPE(double pE) {
			PE = pE;
		}

		public double getKE() {
			return KE;
		}

		public void setKE(double kE) {
			KE = kE;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == null){
				return false;
			}else{
				if(this.getClass() == obj.getClass()){
					Molecule s = (Molecule)obj;
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
}
