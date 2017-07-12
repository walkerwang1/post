package com.paper.read;

/**
 * 三层架构 + 动态cloudlets + 移动性 + 多用户 + DAG图
 * @author walkerwang
 *
 * 怎样在多个cloudlets之间切换？
 * 	获取用户当前位置，判断该用户与多个cloudlets的距离（通过x，y坐标计算）
 */
public class DECM {



	/**
	 * 管理多个cloudlets：每个cloudlet的x和y坐标是固定的
	 * @author walkerwang
	 *
	 */
	private class Cloudlets {
		private int xLoc; 	//cloudlet的x坐标
		
		private int yLoc;	//cloudlet的y坐标
	}
	
	/**
	 * 用户轨迹
	 * @author walkerwang
	 *
	 */
	private class Trajectory {
		private int number;		//第number个位置
		
		private int xLoc;	//x坐标
		
		private int yLoc;	//y坐标
		
		private double cellNet;		//4G网络
		
		private double wifiNet;		//WiFi网络
	}
}