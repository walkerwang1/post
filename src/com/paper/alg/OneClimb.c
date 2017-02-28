#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define TASKNUM 13		//节点数
#define TIMEDELAYNUM 4		//截止时间个数

typedef struct Task {	//任务
	double comp_cloud;			//云端计算时间
	double comp_mobile;			//移动端计算时间
	double earliest_start_time;	//最早开始时间
	double lastest_finish_time;	//最晚结束时间
	double workload;		//工作负载
	int exeLoc;			//执行位置(0-mobile、1-cloud)
	int isScheduled;	//是否被调度（0-没有调度）
	
	struct Task *next; 	//孩子节点
	struct Task *parent;	//父节点
}Task;

typedef struct PCP {	//部分关键路径
	
}PCP;

//-------函数声明-------
void initInputFile();
void printInputFile();
void calESTandLFT(double Td);
void calEST(double Td);
void calLFT(double Td);
void initExeLoc();
double calTransferTime(int i, int j);
int findParentSet(int **ret, int i, double Td);
int findChildSet(int **ret, int i, double Td);
void parentAndChild();
double calCompTime(int n);
double calCompEnergy(int n);
double calTransferTime(int i, int j);
double calTransferTime(int i, int j);
void printESTandLFT();

double **transferData;		//任务之间传输的数据大小
double *workload;			//每个任务的工作负载
double *timedelay;		//截止时间

Task *task;

double send_power = 0.1;		//W
double receive_power = 0.05;		//W
double comp_power = 0.5;		//W
double idle_power = 0.001;		//W
double mobile_frequency = 500;	//MHz
double cloud_frequency = 3000;	//MHz

double bandwidth = 50;		//网络传输速率

int main() 
{
	initInputFile();
	printInputFile();
	
	int i;
	//将任务0和12设置为已经调度
	task[0].isScheduled = 1;
	task[TASKNUM-1].isScheduled = 1;
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//先只考虑截止时间为 0.6s
	{
		double Td = timedelay[i];	//截止时间Td
		printf("--------------------------------------------\n");
		printf("delay deadline:%.1f\n", Td);

		calESTandLFT(Td);
		
		scheduleParents(TASKNUM-1);	  //从最后一个节点开始调度	
	}
	return 0;
}

//计算每个任务的最早开始时间和最晚结束时间
//（除入口/出口节点0,12，假设所有任务都在云端执行，EST和LFT后面会更新）
void calESTandLFT(double Td)
{
	//假设所有任务都在云端执行
	initExeLoc();
	
	calEST(Td);		//最早开始时间EST
	calLFT(Td);		//最晚结束时间LFT
	//输出EST和LFT
	//printESTandLFT();	
}

//计算EST和LFT任务的执行位置
void initExeLoc() 
{
	int i;
	for(i = 1; i < TASKNUM-1; i++)
	{
		task[i].exeLoc = 1;
	}
	parentAndChild();
}

//任务之间的父子关系
void parentAndChild() 
{
	int i, j;
	for(i = 0; i < TASKNUM; i++)
	{
		for(j = i; j < TASKNUM; j++) 
		{
			if(transferData[i][j] > 0) 
			{
				task[i].next = &task[j];
				task[j].parent = &task[i];
			}
		}
	}
}

//计算任务1~12的最早开始时间
void calEST(double Td) 
{
	task[0].earliest_start_time = 0;
	int i, j;
	int *ret;
	double maxEarliestStartTime;
	//计算节点1~12的最早开始时间
	for(i = 1; i < TASKNUM; i++) 
	{
		maxEarliestStartTime = 0;
		int len = findParentSet(&ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int pre = ret[j];		//节点i的后继节点的下标
			double comp_time = calCompTime(pre);
			double transfer_time = calTransferTime(pre, i);
			double time = task[pre].earliest_start_time + comp_time + transfer_time;
			if(time > maxEarliestStartTime)
			{
				maxEarliestStartTime = time;
			}
		}
		task[i].earliest_start_time = maxEarliestStartTime;
	}
}


//计算任务0~11的最晚结束时间
void calLFT(double Td)
{
	int i, j;
	int *ret;
	task[TASKNUM-1].lastest_finish_time = Td;
	double minLastestFinishTime;
	//计算节点1~12的最早开始时间
	for(i = TASKNUM - 2; i >= 0; i--) 
	{
		minLastestFinishTime = 100;
		int len = findChildSet(&ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int succ = ret[j];		//节点i的后继节点的下标
			double comp_time = calCompTime(succ);
			double transfer_time = calTransferTime(i, succ);
			double time = task[succ].lastest_finish_time - comp_time - transfer_time;
			if(time < minLastestFinishTime)
			{
				minLastestFinishTime = time;
			}
		}
		task[i].lastest_finish_time = minLastestFinishTime;
	}
}

//找到n的直接前驱节点
int findParentSet(int **ret, int n, double Td)
{
	int i; 
	int count = 0;
	*ret = (int *)malloc(sizeof(int));	
	for(i = 0; i < TASKNUM; i++) 
	{
		
		if(transferData[i][n] > 0)		
		{
			(*ret)[count++] = i;
		}
	}
	return count;
}


//找到n的直接后继节点
int findChildSet(int **ret, int n, double Td) 
{
	int i;
	*ret = (int *)malloc(sizeof(int));
	int count = 0;
	for(i = 0; i < TASKNUM; i++) 
	{
		if(transferData[n][i] > 0)
		{
			(*ret)[count++] = i;
		}
	}
	return count;
}


//节点n的计算时间
double calCompTime(int n)
{
	double comp_time = 0;
	int exeLoc = task[n].exeLoc;
	if(exeLoc == 0) 
	{
		comp_time = workload[n] / mobile_frequency;
	} 
	else
	{
		comp_time = workload[n] / cloud_frequency;
	}
	return comp_time;
}

//节点i和j之间的传输时间
double calTransferTime(int i, int j) 
{
	double transfer_time = 0;
	if(task[i].exeLoc == task[j].exeLoc)
	{
		transfer_time = 0;
	}
	else
	{
		transfer_time = transferData[i][j] / bandwidth;
	}
	return transfer_time;
}

/*
//从节点n开始调度
void scheduleParents(int n) 
{
	
}

//节点n的计算能耗
double calCompEnergy(int n) 
{
	double comp_energy;
	int exeLoc = task[n].exeLoc;
	double comp_time = calCompTime(n);
	if(exeLoc == 0)
	{
		comp_energy = comp_time * comp_power;		//本地执行，计算能耗
	} 
	else
	{
		comp_energy = comp_time * idle_power;		//云端执行，空闲能耗
	}
	return comp_energy;
}

//节点i和j之间的传输能耗
double calTransferEnergy(int i, int j)
{
	double transfer_energy = 0;
	double transfer_time = calTransferTime(i, j);
	if(task[i].exeLoc == task[j].exeLoc)
	{
		transfer_energy = 0;
	}
	if(task[i].exeLoc == 0 && task[j].exeLoc == 1)
	{
		transfer_energy = transfer_time * send_power;
	}
	if(task[i].exeLoc == 1 && task[j].exeLoc == 0)
	{
		transfer_energy = transfer_time * receive_power;
	}
	return transfer_energy;
}
*/

//更新任务的最早开始时间
void updateEST() 
{
	
}

//更新任务的最晚结束时间
void updateLFT()
{
	
}

//读取输入信息
void initInputFile() 
{
	FILE *fp_file;
	int i,j;
	//初始化邻接矩阵的值
	transferData = (double **)malloc(sizeof(double *)*TASKNUM);
	for (i = 0; i < TASKNUM; i++)
	{
		transferData[i] = (double *)malloc(sizeof(double)*TASKNUM);
	}
	fp_file = fopen("mesh.dat", "r");
	for (i = 0; i < TASKNUM; i++) 
	{
		for (j = 0; j < TASKNUM; j++) 
		{
			fscanf(fp_file, "%lf", &transferData[i][j]);
		}			
	}
	fclose(fp_file);
	//task
	task = (Task *)malloc(sizeof(Task)*TASKNUM);
	//工作负载
	workload = (double *)malloc(sizeof(double)*TASKNUM);
	fp_file = fopen("workload.dat", "r");
	for(i = 0; i < TASKNUM; i++) 
	{
		fscanf(fp_file, "%lf", &workload[i]);
		//fscanf(fp_file, "%lf", &task[i].workload);
	}
	//截止时间
	timedelay = (double *)malloc(sizeof(double)*TIMEDELAYNUM);
	for(i = 0; i < TIMEDELAYNUM; i++)
	{
		fscanf(fp_file, "%lf", &timedelay[i]);
	}
	for(i = 0; i < TIMEDELAYNUM; i++)
	{
		task[i].workload = workload[i];
	}
	
	fclose(fp_file);
}

//输出最早完成时间和最晚开始时间
void printESTandLFT()
{
	int i;
	for(i = 0; i < TASKNUM; i++)
	{
		printf("%2d : EST:%.5f    LFT:%.5f\n", i, task[i].earliest_start_time, task[i].lastest_finish_time);
	}
}

//输出信息
void printInputFile()
{
	int i, j;
	printf("边信息：\n");
	for(i = 0; i < TASKNUM; i++) 
	{
		for(j = 0; j <TASKNUM; j++)
		{
			printf("%.1f\t", transferData[i][j]);
		}
		printf("\n");
	}
	printf("负载信息：\n");
	for(i = 0; i < TASKNUM; i++) 
	{
		printf("%.1f\t", workload[i]);
	}
	printf("\n");
	printf("截止时间：\n");
	for(i = 0; i < TIMEDELAYNUM; i++) 
	{
		printf("%.1f\t", timedelay[i]);
	}
	printf("\n");
}