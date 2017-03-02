#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define TASKNUM 13		//节点数
#define TIMEDELAYNUM 4		//截止时间个数

typedef struct Task {	
	double comp_cloud;			//云端计算时间
	double comp_mobile;			//移动端计算时间
	double earliest_start_time;	//最早开始时间
	double lastest_finish_time;	//最晚结束时间
	double workload;		//工作负载
	int exeLoc;			//执行位置(0-mobile、1-cloud)
	int isScheduled;	//是否被调度（0-没有调度）
	
	struct Task *next; 	//孩子节点
	struct Task *parent;	//父节点
}Task;	//任务

typedef struct PCPList {	
	int value;
	struct PCPList *next;
	struct PCPList *pre;
}PCPList;	//部分关键路径，线性链表

//--------------函数声明--------------
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
void scheduleParents(int n, double Td);
int isUnscheduledParent(int v);
void initPCPList(PCPList **pcpHead);
void addTaskToPCP(int n);
int findCriticalParentScheduled(int u);
int findCriticalParentUnscheduled(int u);
void printPCP(PCPList *pcpHead);
void schedulePath(PCPList *pcpHead);
double allTaskExeOnMobile(PCPList *pcpHead, int first, int last, double sub_deadline);
double oneMigrationToCloud(PCPList *pcpHead, int first, int last, double sub_deadline);
double calOneMigrationEnergy(PCPList *pcpHead, int first, int last, int n);
int findLastPCP(PCPList *pcpHead);
double calTransferEnergy(int i, int j);
void clearExeLoc();
void updateEST(double Td);
void updateLFT(double Td);

double **transferData;		//任务之间传输的数据大小
double *workload;		//每个任务的工作负载
double *timedelay;		//截止时间

Task *task;
PCPList *pcpHead = NULL;	//PCP路径链表的头节点
PCPList *pcpRear;		//PCP路径链表的尾节点

int startMigrationID = 0;		//记录第一次开始迁移的任务id

double send_power = 0.1;		//W
double receive_power = 0.05;		//W
double comp_power = 0.5;		//W
double idle_power = 0.001;		//W
double mobile_frequency = 500;		//MHz
double cloud_frequency = 3000;		//MHz

double bandwidth = 50;		//网络传输速率

int main() 
{
	initInputFile();
	//printInputFile();
	
	int i;
	//将任务0和12设置为已经调度
	task[0].isScheduled = 1;
	task[TASKNUM-1].isScheduled = 1;
	//printInputFile();
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//先只考虑截止时间为 0.6s
	{
		double Td = timedelay[i];	//截止时间Td
		printf("-------------------------- ");
		printf("delay deadline:%.1f ----------------------------\n", Td);

		calESTandLFT(Td);

		//输出EST和LFT
		printESTandLFT();		

		scheduleParents(TASKNUM-1, Td);	  //从最后一个节点开始调度	
		//printESTandLFT();
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
	
	clearExeLoc();
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

void clearExeLoc()
{
	int i;
	for(i = 1; i < TASKNUM-1; i++)
	{
		task[i].exeLoc = 0;
	}
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

//从节点n开始调度
void scheduleParents(int v, double Td) 
{
	int i =0;
	//初始化PCP链表
	initPCPList(&pcpHead);	
	//1-表示v有未调度的父亲
	while (isUnscheduledParent(v) == 1)
	{
		addTaskToPCP(v);	//尾插法，（注意PCP链表中的调用顺序）
		int u = v;
		
		while(isUnscheduledParent(u) == 1)
		{
			//找到u的未被调度的关键父节点
			int w = findCriticalParentUnscheduled(u);
			
			addTaskToPCP(w);
			u = w;
		}
		
		//u是PCP路径上的倒数第二个节点，找到u的已经调度的关键父节点，此时构成了一条PCP路径
		int scheduled_parent = findCriticalParentScheduled(u);
		addTaskToPCP(scheduled_parent);
		//-------至此，一条PCP路径就构成---------
		
		//打印PCP路径上包含的节点(to this, correct)
		printPCP(pcpHead);	
		
		
		//开始调度PCP路径上的每一个节点
		schedulePath(pcpHead);		//pcpHead为PCP路径链表的头节点

		//更新之前打印PCP上每个任务的卸载位置（*）
		updateEST(Td);

		updateLFT(Td);		

		printf("\n");
		printESTandLFT();
		
		

		/*
		//PCP路径上每一个节点依次调用scheduleParents
		PCPList *p = pcpHead->next;
		PCPList *q = NULL;	
		while(p != NULL)
		{
			scheduleParents(p->value, Td);
			q = p;
			p = p->next;
			free(q);
		}	
		*/
	}
}

//判断v是否有未调度的父节点(1-表示有未调度的)
int isUnscheduledParent(int v)
{
	int i;
	int *ret;
	int len = findParentSet(&ret, v, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];
		if(task[parent].isScheduled == 0)	//i没有被调度
		{
			return 1;
		}
	}
	return 0;
}

//找到u的未调度的关键父节点（根据EST）
int findCriticalParentUnscheduled(int u)
{
	int i;
	int *ret;
	double maxEST = -1;
	int index;		//存放关键父节点的id
	int len = findParentSet(&ret, u, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];	//节点u的父节点的id
		
		//父节点没有被调度，并找到critical parent
		if(task[parent].isScheduled == 0)
		{
			if(task[parent].earliest_start_time > maxEST)
			{
				maxEST = task[parent].earliest_start_time;
				index = parent;
			}
		}
	}
	return index;
}

//找到u的已经被调度的关键父节点
int findCriticalParentScheduled(int u)
{
	int i;
	int *ret;
	double maxEST = -1;
	int index;		//存放关键父节点的id
	int len = findParentSet(&ret, u, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];	//节点u的父节点的id
		
		//父节点已经被调度，并找到critical parent
		if(task[parent].isScheduled == 1)
		{
			if(task[parent].earliest_start_time > maxEST)
			{
				maxEST = task[parent].earliest_start_time;
				index = parent;
			}
		}
	}
	return index;
}

//调度PCP关键路径上的每一个节点(Alg 3)
void schedulePath(PCPList *pcpHead)
{
	//12-11-9-6-3-1-0
	int first  = pcpHead->value;		//PCP第一个节点
	int last = findLastPCP(pcpHead);		//PCP最后一个节点
	//printf("first node:%d;\t last node:%d\n", first, last);
	
	//子截止时间
	double sub_deadline = task[last].lastest_finish_time - task[first].earliest_start_time;
	
	int firstExeLoc = task[first].exeLoc;
	int  lastExeLoc = task[last].exeLoc;
	if(firstExeLoc == 0 && lastExeLoc == 0)
	{
		//1.所有任务都在本地执行
		double energy_all_mobile = allTaskExeOnMobile(pcpHead, first, last, sub_deadline);

		printf("energy_all_mobile:%.5f\n", energy_all_mobile);		//0.149 (correct)

		//2.从移动端迁移一次到云端
		double energy_one_migration_cloud = oneMigrationToCloud(pcpHead, first, last, sub_deadline);
		
		printf("energy_one_migration_cloud:%.5f\n", energy_one_migration_cloud);		//0.042045 (correct)
		
		printf("startMigrationID:%d\n", startMigrationID);		//startMigrationID = 6 (correct)
		
		//表明所有任务在本地执行更优
		if(energy_all_mobile <= energy_one_migration_cloud)
		{
			PCPList *p = pcpHead->next;
			while(p != NULL)
			{
				if(p->value != first && p->value != last)
				{
					task[p->value].exeLoc = 0;
				}
				p = p->next;
			}
		}
		else	//表明一次迁移到云端更优
		{
			//startMigrationID表示从哪个节点开始卸载
			PCPList *p = pcpHead->next;
			while(p != NULL) 
			{
				if(p->value != first && p->value != last)
				{
					if(p->value < startMigrationID)
						task[p->value].exeLoc = 0;
					else
						task[p->value].exeLoc = 1;
				}
				p = p->next;
			}
		}
	}
/*
	//设置first+1到last-1中的某一个任务迁移到云端
	else if(firstExeLoc == 0 && lastExeLoc == 1)
	{
		
	}
	else if(firstExeLoc ==1 && lastExeLoc == 0)
	{
		
	}
	else	//first和last都在云端执行，则所有节点都在云端执行
	{
		PCPList *p = pcpHead->next;
		while(p != NULL) 
		{
			if(p->value != first && p->value != last)
			{
				task[p->value].exeLoc = 1;
			}
			p = p->next;
		}
	}
*/
	//将PCP路径上的节点设置为“已调度”
	PCPList *p = pcpHead->next;
	int w = 0;
	while(p != NULL)
	{
		w = p->value;
		
		task[w].isScheduled = 1;
		p = p->next;
	}
	
	//Update X：将PCP上的节点都确定其执行位置exeLoc
	
}

//所有组件都在移动端执行
double allTaskExeOnMobile(PCPList *pcpHead, int first, int last, double sub_deadline)
{
	int i;
	double energy_all_mobile = 0;
	
	//假设
	PCPList *p = pcpHead->next;
	while(p != NULL)
	{
		if(p->value != first && p->value != last )
		{
			//设置所有任务都在移动端执行
			task[p->value].exeLoc = 0;
		}
		p = p->next;
	}
	
	//计算该卸载方式下的能耗
	p = pcpHead->next;
	while(p != NULL)
	{
		int n = p->value;
		double comp_energy = calCompEnergy(n);
		double transfer_energy = 0;
		if(p->pre != NULL)
		{
			transfer_energy = calTransferEnergy(p->pre->value, n);
		}
		energy_all_mobile += comp_energy;
		energy_all_mobile += transfer_energy;

		p = p->next;
	}
	
	//清楚task中的假设执行位置
	p = pcpHead->next;
	while(p != NULL)
	{
		if(p->value != first && p->value != last)
		{
			task[p->value].exeLoc = 0;	
		}
		p = p->next;
	}
	return energy_all_mobile;
}

//从移动端迁移一次到云端，first和last的exeLoc的执行位置已知
double oneMigrationToCloud(PCPList *pcpHead, int first, int last, double sub_deadline)
{
	//double energy_one_migration_cloud = 0;
	double minExeEnergy = 1000;
	startMigrationID = first + 1;
	PCPList *p = pcpHead->next;
	while(p != NULL) 
	{
		double exeEnergy = 0;
		if(p->value != first && p->value != last)
		{
			exeEnergy = calOneMigrationEnergy(pcpHead, first, last, p->value);
			if(exeEnergy < minExeEnergy)
			{
				minExeEnergy = exeEnergy;
				startMigrationID = p->value;
			}
		}
			
		p = p->next;
	}
	return minExeEnergy;
}

//从n开始卸载的能耗
double calOneMigrationEnergy(PCPList *pcpHead, int first, int last, int n)
{
	double exeEnergy = 0;
	PCPList *p = pcpHead->next;
	while(p != NULL) 
	{
		if(p->value != first && p->value != last)
		{
			if(p->value < n)	//从n开始卸载
			{
				task[p->value].exeLoc = 0;
			}
			else
			{
				task[p->value].exeLoc = 1;
			}
		}
		
		
		p = p->next;	
	}
	
	//开始计算能耗
	p = pcpHead->next;
	while(p != NULL)
	{
		int n = p->value;
		double comp_energy = calCompEnergy(n);
		double transfer_energy = 0;
		if(p->pre != NULL)
		{
			transfer_energy = calTransferEnergy(p->pre->value, n);
		}
		exeEnergy += comp_energy;
		exeEnergy += transfer_energy;

		p = p->next;
	}
	
	//清除task中的假设执行位置
	p = pcpHead->next;
	while(p != NULL)
	{
		if(p->value != first && p->value != last)
		{
			task[p->value].exeLoc = 0;	
		}

		p = p->next;
	}
	return exeEnergy;
}

//更新任务的最早开始时间
void updateEST(double Td) 
{
	calEST(Td);
}

//更新任务的最晚结束时间
void updateLFT(double Td)
{
	calLFT(Td);
}

//将节点n添加到PCP路径中(头插法)
void addTaskToPCP(int n)
{
	PCPList *p = (PCPList *)malloc(sizeof(PCPList));
	p->value = n;
	if(pcpHead->next != NULL)
	{
		p->next = pcpHead->next;
		pcpHead->next->pre = p;
		pcpHead->next = p;
		p->pre = pcpHead;
	} 
	else
	{
		pcpHead->next = p;
		p->pre = pcpHead;
		p->next = NULL;
	}
}

//找到PCP最后一个节点
int findLastPCP(PCPList *pcpHead)
{
	PCPList *p = pcpHead->next;
	while(p->next != NULL)
	{
		p = p->next;
	}
	return p->value;
}

//初始化PCPList链表，只有头节点
void initPCPList(PCPList **pcpHead) 
{
	(*pcpHead) = (PCPList *)malloc(sizeof(PCPList));
	(*pcpHead)->next = NULL;
	(*pcpHead)->pre = NULL;
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
	if(task[i].exeLoc == task[j].exeLoc)	//i,j在同一个地方执行
	{
		transfer_energy = 0;
	}
	if(task[i].exeLoc == 0 && task[j].exeLoc == 1)	//i移动端，j云端
	{
		transfer_energy = transfer_time * send_power;
	}
	if(task[i].exeLoc == 1 && task[j].exeLoc == 0)	//i云端，j移动端
	{
		transfer_energy = transfer_time * receive_power;
	}
	return transfer_energy;
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
	for(i = 0; i < TASKNUM; i++)
	{
		task[i].isScheduled = 0;
	}
	
	fclose(fp_file);
}

//输出最早完成时间和最晚开始时间
void printESTandLFT()
{
	int i;
	for(i = 0; i < TASKNUM; i++)
	{
		printf("%2d : exeLoc:%d  EST:%.5f   LFT:%.5f   isScheduled:%d\n", i, task[i].exeLoc, task[i].earliest_start_time, task[i].lastest_finish_time, task[i].isScheduled);
	}
}

//打印PCP部分关键路径
void printPCP(PCPList *pcpHead)
{
	PCPList *p = pcpHead->next;
	printf("PCP路径为：");
	while(p != NULL) 
	{
		printf("%d  ", p->value);
		p = p->next;
	}
	printf("\n");
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
	for(i = 0; i < TASKNUM; i++)
	{
		printf("%d\t", task[i].isScheduled);
	}
	printf("\n");
}