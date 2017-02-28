#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define TASKNUM 13		//�ڵ���
#define TIMEDELAYNUM 4		//��ֹʱ�����

typedef struct Task {	//����
	double comp_cloud;			//�ƶ˼���ʱ��
	double comp_mobile;			//�ƶ��˼���ʱ��
	double earliest_start_time;	//���翪ʼʱ��
	double lastest_finish_time;	//�������ʱ��
	double workload;		//��������
	int exeLoc;			//ִ��λ��(0-mobile��1-cloud)
	int isScheduled;	//�Ƿ񱻵��ȣ�0-û�е��ȣ�
	
	struct Task *next; 	//���ӽڵ�
	struct Task *parent;	//���ڵ�
}Task;

typedef struct PCP {	//���ֹؼ�·��
	
}PCP;

//-------��������-------
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

double **transferData;		//����֮�䴫������ݴ�С
double *workload;			//ÿ������Ĺ�������
double *timedelay;		//��ֹʱ��

Task *task;

double send_power = 0.1;		//W
double receive_power = 0.05;		//W
double comp_power = 0.5;		//W
double idle_power = 0.001;		//W
double mobile_frequency = 500;	//MHz
double cloud_frequency = 3000;	//MHz

double bandwidth = 50;		//���紫������

int main() 
{
	initInputFile();
	printInputFile();
	
	int i;
	//������0��12����Ϊ�Ѿ�����
	task[0].isScheduled = 1;
	task[TASKNUM-1].isScheduled = 1;
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//��ֻ���ǽ�ֹʱ��Ϊ 0.6s
	{
		double Td = timedelay[i];	//��ֹʱ��Td
		printf("--------------------------------------------\n");
		printf("delay deadline:%.1f\n", Td);

		calESTandLFT(Td);
		
		scheduleParents(TASKNUM-1);	  //�����һ���ڵ㿪ʼ����	
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
	//printESTandLFT();	
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

/*
//�ӽڵ�n��ʼ����
void scheduleParents(int n) 
{
	
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

//������������翪ʼʱ��
void updateEST() 
{
	
}

//����������������ʱ��
void updateLFT()
{
	
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
}