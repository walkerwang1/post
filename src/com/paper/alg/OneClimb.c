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

double **transferData;		//����֮�䴫������ݴ�С
double *workload;		//ÿ������Ĺ�������
double *timedelay;		//��ֹʱ��

Task *task;
PCPList *pcpHead = NULL;	//PCP·�������ͷ�ڵ�
PCPList *pcpRear;		//PCP·�������β�ڵ�

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
	printInputFile();
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//��ֻ���ǽ�ֹʱ��Ϊ 0.6s
	{
		double Td = timedelay[i];	//��ֹʱ��Td
		printf("--------------------------------------------\n");
		printf("delay deadline:%.1f\n", Td);

		calESTandLFT(Td);
		
		scheduleParents(TASKNUM-1, Td);	  //�����һ���ڵ㿪ʼ����	
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
	//���EST��LFT
	printESTandLFT();	
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
				
				//�ҵ�u�Ĺؼ����ڵ㣬����������Ϊ���ѵ��ȡ�
				task[w].isScheduled = 1;
				
				addTaskToPCP(w);
				u = w;
		}
		
		//u��PCP·���ϵĵ����ڶ����ڵ㣬�ҵ�u���Ѿ����ȵĹؼ����ڵ㣬��ʱ������һ��PCP·��
		int scheduled_parent = findCriticalParentScheduled(u);
		addTaskToPCP(scheduled_parent);
		//-------���ˣ�һ��PCP·���͹���---------
		
		//��ӡPCP·���ϰ����Ľڵ�(to this, correct)
		printPCP(pcpHead);	
		
		/*
		//��ʼ����PCP·���ϵ�ÿһ���ڵ�
		schedulePath(pcpHead);		//pcpHeadΪPCP·�������ͷ�ڵ�
		
		//����֮ǰ��ӡPCP��ÿ�������ж��λ�ã�*��
		//PCP(12-11-9-6-3-1-0)  6,9,10,10���ƶ�1�������Ķ����ƶ���ִ��
		updateEST();
		
		updateLFT();
		
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

//����PCP�ؼ�·���ϵ�ÿһ���ڵ�
void schedulePath(PCP)
{
	//12-11-9-6-3-1-0���е㲻̫�������Ƿ���Ҫ����Ϊ��˫������
	int last  = pcpHead->value;		//PCP���һ���ڵ�
	int first = pcpRear->value;		//PCP��һ���ڵ�
}


//������������翪ʼʱ��
void updateEST() 
{
	
}

//����������������ʱ��
void updateLFT()
{
	
}

//���ڵ�n��ӵ�PCP·����(β�巨)
void addTaskToPCP(int n)
{
	PCPList *p = (PCPList *)malloc(sizeof(PCPList));
	p->value = n;
	if(pcpHead->next != NULL)
	{
		pcpRear->next = p;
		pcpRear = p;
	} 
	else
	{
		pcpHead->next = p;
		pcpRear = p;
	}
	pcpRear->next = NULL;
}

//��ʼ��PCPList����ֻ��ͷ�ڵ�
void initPCPList(PCPList **pcpHead) 
{
	(*pcpHead) = (PCPList *)malloc(sizeof(PCPList));
	(*pcpHead)->next = NULL;
	pcpRear = (*pcpHead);
	pcpRear->next = NULL;
}

/*


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
		printf("%2d : EST:%.5f    LFT:%.5f\n", i, task[i].earliest_start_time, task[i].lastest_finish_time);
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