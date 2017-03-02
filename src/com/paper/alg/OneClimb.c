#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define TASKNUM 13		//�ڵ���
#define TIMEDELAYNUM 4		//��ֹʱ�����

typedef struct Task {	
	double comp_cloud;			//�ƶ˼���ʱ��
	double comp_mobile;			//�ƶ��˼���ʱ��
	double earliest_start_time;	//���翪ʼʱ��
	double lastest_finish_time;	//�������ʱ��
	double workload;		//��������
	int exeLoc;			//ִ��λ��(0-mobile��1-cloud)
	int isScheduled;	//�Ƿ񱻵��ȣ�0-û�е��ȣ�
	
	struct Task *next; 	//���ӽڵ�
	struct Task *parent;	//���ڵ�
}Task;	//����

typedef struct PCPList {	
	int value;
	struct PCPList *next;
	struct PCPList *pre;
}PCPList;	//���ֹؼ�·������������

//--------------��������--------------
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

double **transferData;		//����֮�䴫������ݴ�С
double *workload;		//ÿ������Ĺ�������
double *timedelay;		//��ֹʱ��

Task *task;
PCPList *pcpHead = NULL;	//PCP·�������ͷ�ڵ�
PCPList *pcpRear;		//PCP·�������β�ڵ�

int startMigrationID = 0;		//��¼��һ�ο�ʼǨ�Ƶ�����id

double send_power = 0.1;		//W
double receive_power = 0.05;		//W
double comp_power = 0.5;		//W
double idle_power = 0.001;		//W
double mobile_frequency = 500;		//MHz
double cloud_frequency = 3000;		//MHz

double bandwidth = 50;		//���紫������

int main() 
{
	initInputFile();
	//printInputFile();
	
	int i;
	//������0��12����Ϊ�Ѿ�����
	task[0].isScheduled = 1;
	task[TASKNUM-1].isScheduled = 1;
	//printInputFile();
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//��ֻ���ǽ�ֹʱ��Ϊ 0.6s
	{
		double Td = timedelay[i];	//��ֹʱ��Td
		printf("-------------------------- ");
		printf("delay deadline:%.1f ----------------------------\n", Td);

		calESTandLFT(Td);

		//���EST��LFT
		printESTandLFT();		

		scheduleParents(TASKNUM-1, Td);	  //�����һ���ڵ㿪ʼ����	
		//printESTandLFT();
	}
	return 0;
}

//����ÿ����������翪ʼʱ����������ʱ��
//�������/���ڽڵ�0,12�����������������ƶ�ִ�У�EST��LFT�������£�
void calESTandLFT(double Td)
{
	//���������������ƶ�ִ��
	initExeLoc();
	
	calEST(Td);		//���翪ʼʱ��EST
	calLFT(Td);		//�������ʱ��LFT
	
	clearExeLoc();
}

//����EST��LFT�����ִ��λ��
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

//����֮��ĸ��ӹ�ϵ
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

//��������1~12�����翪ʼʱ��
void calEST(double Td) 
{
	task[0].earliest_start_time = 0;
	int i, j;
	int *ret;
	double maxEarliestStartTime;
	//����ڵ�1~12�����翪ʼʱ��
	for(i = 1; i < TASKNUM; i++) 
	{
		maxEarliestStartTime = 0;
		int len = findParentSet(&ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int pre = ret[j];		//�ڵ�i�ĺ�̽ڵ���±�
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


//��������0~11���������ʱ��
void calLFT(double Td)
{
	int i, j;
	int *ret;
	task[TASKNUM-1].lastest_finish_time = Td;
	double minLastestFinishTime;
	//����ڵ�1~12�����翪ʼʱ��
	for(i = TASKNUM - 2; i >= 0; i--) 
	{
		minLastestFinishTime = 100;
		int len = findChildSet(&ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int succ = ret[j];		//�ڵ�i�ĺ�̽ڵ���±�
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

//�ҵ�n��ֱ��ǰ���ڵ�
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


//�ҵ�n��ֱ�Ӻ�̽ڵ�
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


//�ڵ�n�ļ���ʱ��
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

//�ڵ�i��j֮��Ĵ���ʱ��
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

//�ӽڵ�n��ʼ����
void scheduleParents(int v, double Td) 
{
	int i =0;
	//��ʼ��PCP����
	initPCPList(&pcpHead);	
	//1-��ʾv��δ���ȵĸ���
	while (isUnscheduledParent(v) == 1)
	{
		addTaskToPCP(v);	//β�巨����ע��PCP�����еĵ���˳��
		int u = v;
		
		while(isUnscheduledParent(u) == 1)
		{
			//�ҵ�u��δ�����ȵĹؼ����ڵ�
			int w = findCriticalParentUnscheduled(u);
			
			addTaskToPCP(w);
			u = w;
		}
		
		//u��PCP·���ϵĵ����ڶ����ڵ㣬�ҵ�u���Ѿ����ȵĹؼ����ڵ㣬��ʱ������һ��PCP·��
		int scheduled_parent = findCriticalParentScheduled(u);
		addTaskToPCP(scheduled_parent);
		//-------���ˣ�һ��PCP·���͹���---------
		
		//��ӡPCP·���ϰ����Ľڵ�(to this, correct)
		printPCP(pcpHead);	
		
		
		//��ʼ����PCP·���ϵ�ÿһ���ڵ�
		schedulePath(pcpHead);		//pcpHeadΪPCP·�������ͷ�ڵ�

		//����֮ǰ��ӡPCP��ÿ�������ж��λ�ã�*��
		updateEST(Td);

		updateLFT(Td);		

		printf("\n");
		printESTandLFT();
		
		

		/*
		//PCP·����ÿһ���ڵ����ε���scheduleParents
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

//�ж�v�Ƿ���δ���ȵĸ��ڵ�(1-��ʾ��δ���ȵ�)
int isUnscheduledParent(int v)
{
	int i;
	int *ret;
	int len = findParentSet(&ret, v, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];
		if(task[parent].isScheduled == 0)	//iû�б�����
		{
			return 1;
		}
	}
	return 0;
}

//�ҵ�u��δ���ȵĹؼ����ڵ㣨����EST��
int findCriticalParentUnscheduled(int u)
{
	int i;
	int *ret;
	double maxEST = -1;
	int index;		//��Źؼ����ڵ��id
	int len = findParentSet(&ret, u, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];	//�ڵ�u�ĸ��ڵ��id
		
		//���ڵ�û�б����ȣ����ҵ�critical parent
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

//�ҵ�u���Ѿ������ȵĹؼ����ڵ�
int findCriticalParentScheduled(int u)
{
	int i;
	int *ret;
	double maxEST = -1;
	int index;		//��Źؼ����ڵ��id
	int len = findParentSet(&ret, u, 0);
	for(i = 0; i < len; i++)
	{
		int parent = ret[i];	//�ڵ�u�ĸ��ڵ��id
		
		//���ڵ��Ѿ������ȣ����ҵ�critical parent
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

//����PCP�ؼ�·���ϵ�ÿһ���ڵ�(Alg 3)
void schedulePath(PCPList *pcpHead)
{
	//12-11-9-6-3-1-0
	int first  = pcpHead->value;		//PCP��һ���ڵ�
	int last = findLastPCP(pcpHead);		//PCP���һ���ڵ�
	//printf("first node:%d;\t last node:%d\n", first, last);
	
	//�ӽ�ֹʱ��
	double sub_deadline = task[last].lastest_finish_time - task[first].earliest_start_time;
	
	int firstExeLoc = task[first].exeLoc;
	int  lastExeLoc = task[last].exeLoc;
	if(firstExeLoc == 0 && lastExeLoc == 0)
	{
		//1.���������ڱ���ִ��
		double energy_all_mobile = allTaskExeOnMobile(pcpHead, first, last, sub_deadline);

		printf("energy_all_mobile:%.5f\n", energy_all_mobile);		//0.149 (correct)

		//2.���ƶ���Ǩ��һ�ε��ƶ�
		double energy_one_migration_cloud = oneMigrationToCloud(pcpHead, first, last, sub_deadline);
		
		printf("energy_one_migration_cloud:%.5f\n", energy_one_migration_cloud);		//0.042045 (correct)
		
		printf("startMigrationID:%d\n", startMigrationID);		//startMigrationID = 6 (correct)
		
		//�������������ڱ���ִ�и���
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
		else	//����һ��Ǩ�Ƶ��ƶ˸���
		{
			//startMigrationID��ʾ���ĸ��ڵ㿪ʼж��
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
	//����first+1��last-1�е�ĳһ������Ǩ�Ƶ��ƶ�
	else if(firstExeLoc == 0 && lastExeLoc == 1)
	{
		
	}
	else if(firstExeLoc ==1 && lastExeLoc == 0)
	{
		
	}
	else	//first��last�����ƶ�ִ�У������нڵ㶼���ƶ�ִ��
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
	//��PCP·���ϵĽڵ�����Ϊ���ѵ��ȡ�
	PCPList *p = pcpHead->next;
	int w = 0;
	while(p != NULL)
	{
		w = p->value;
		
		task[w].isScheduled = 1;
		p = p->next;
	}
	
	//Update X����PCP�ϵĽڵ㶼ȷ����ִ��λ��exeLoc
	
}

//������������ƶ���ִ��
double allTaskExeOnMobile(PCPList *pcpHead, int first, int last, double sub_deadline)
{
	int i;
	double energy_all_mobile = 0;
	
	//����
	PCPList *p = pcpHead->next;
	while(p != NULL)
	{
		if(p->value != first && p->value != last )
		{
			//���������������ƶ���ִ��
			task[p->value].exeLoc = 0;
		}
		p = p->next;
	}
	
	//�����ж�ط�ʽ�µ��ܺ�
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
	
	//���task�еļ���ִ��λ��
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

//���ƶ���Ǩ��һ�ε��ƶˣ�first��last��exeLoc��ִ��λ����֪
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

//��n��ʼж�ص��ܺ�
double calOneMigrationEnergy(PCPList *pcpHead, int first, int last, int n)
{
	double exeEnergy = 0;
	PCPList *p = pcpHead->next;
	while(p != NULL) 
	{
		if(p->value != first && p->value != last)
		{
			if(p->value < n)	//��n��ʼж��
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
	
	//��ʼ�����ܺ�
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
	
	//���task�еļ���ִ��λ��
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

//������������翪ʼʱ��
void updateEST(double Td) 
{
	calEST(Td);
}

//����������������ʱ��
void updateLFT(double Td)
{
	calLFT(Td);
}

//���ڵ�n��ӵ�PCP·����(ͷ�巨)
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

//�ҵ�PCP���һ���ڵ�
int findLastPCP(PCPList *pcpHead)
{
	PCPList *p = pcpHead->next;
	while(p->next != NULL)
	{
		p = p->next;
	}
	return p->value;
}

//��ʼ��PCPList����ֻ��ͷ�ڵ�
void initPCPList(PCPList **pcpHead) 
{
	(*pcpHead) = (PCPList *)malloc(sizeof(PCPList));
	(*pcpHead)->next = NULL;
	(*pcpHead)->pre = NULL;
}


//�ڵ�n�ļ����ܺ�
double calCompEnergy(int n) 
{
	double comp_energy;
	int exeLoc = task[n].exeLoc;
	double comp_time = calCompTime(n);
	if(exeLoc == 0)
	{
		comp_energy = comp_time * comp_power;		//����ִ�У������ܺ�
	} 
	else
	{
		comp_energy = comp_time * idle_power;		//�ƶ�ִ�У������ܺ�
	}
	return comp_energy;
}

//�ڵ�i��j֮��Ĵ����ܺ�
double calTransferEnergy(int i, int j)
{
	double transfer_energy = 0;
	double transfer_time = calTransferTime(i, j);
	if(task[i].exeLoc == task[j].exeLoc)	//i,j��ͬһ���ط�ִ��
	{
		transfer_energy = 0;
	}
	if(task[i].exeLoc == 0 && task[j].exeLoc == 1)	//i�ƶ��ˣ�j�ƶ�
	{
		transfer_energy = transfer_time * send_power;
	}
	if(task[i].exeLoc == 1 && task[j].exeLoc == 0)	//i�ƶˣ�j�ƶ���
	{
		transfer_energy = transfer_time * receive_power;
	}
	return transfer_energy;
}


//��ȡ������Ϣ
void initInputFile() 
{
	FILE *fp_file;
	int i,j;
	//��ʼ���ڽӾ����ֵ
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
	//��������
	workload = (double *)malloc(sizeof(double)*TASKNUM);
	fp_file = fopen("workload.dat", "r");
	for(i = 0; i < TASKNUM; i++) 
	{
		fscanf(fp_file, "%lf", &workload[i]);
		//fscanf(fp_file, "%lf", &task[i].workload);
	}
	//��ֹʱ��
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

//����������ʱ�������ʼʱ��
void printESTandLFT()
{
	int i;
	for(i = 0; i < TASKNUM; i++)
	{
		printf("%2d : exeLoc:%d  EST:%.5f   LFT:%.5f   isScheduled:%d\n", i, task[i].exeLoc, task[i].earliest_start_time, task[i].lastest_finish_time, task[i].isScheduled);
	}
}

//��ӡPCP���ֹؼ�·��
void printPCP(PCPList *pcpHead)
{
	PCPList *p = pcpHead->next;
	printf("PCP·��Ϊ��");
	while(p != NULL) 
	{
		printf("%d  ", p->value);
		p = p->next;
	}
	printf("\n");
}

//�����Ϣ
void printInputFile()
{
	int i, j;
	printf("����Ϣ��\n");
	for(i = 0; i < TASKNUM; i++) 
	{
		for(j = 0; j <TASKNUM; j++)
		{
			printf("%.1f\t", transferData[i][j]);
		}
		printf("\n");
	}
	printf("������Ϣ��\n");
	for(i = 0; i < TASKNUM; i++) 
	{
		printf("%.1f\t", workload[i]);
	}
	printf("\n");
	printf("��ֹʱ�䣺\n");
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