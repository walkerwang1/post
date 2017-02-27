#include <stdio.h>
#include <stdlib.h>

#define TASKNUM 13		//�ڵ���
#define TIMEDELAYNUM 4		//��ֹʱ�����

typedef struct Task {	//����
	double comp_cloud;			//�ƶ˼���ʱ��
	double comp_mobile;			//�ƶ��˼���ʱ��
	double earliest_start_time;	//���翪ʼʱ��
	double lastest_finish_time;	//�������ʱ��
	
	double workload;	//��������
	
	int exeLoc;			//ִ��λ��(0-mobile��1-cloud)
	
	int isScheduled;	//�Ƿ񱻵��ȣ�0-û�е��ȣ�
	
	struct Task next; 	//���ӽڵ�
	struct Task parent;	//���ڵ�
} Task��

typedef struct PCP {	//���ֹؼ�·��
	
} PCP;

//-------��������-------
void initInputFile();
void printInputFile();

double **transferData;		//����֮�䴫������ݴ�С
double *workload;			//ÿ������Ĺ�������
double *timedelay;		//��ֹʱ��

Task[] task = new Task[TASKNUM];

double send_power = 0.1;		//W
double receive_power = 0.05;	//W
double comp_power = 0.5;		//W
double idle_power = 0.001;		//W
double mobile_frequency = 500;	//MHz
double cloud_frequency = 3000;	//MHz

double bandwidth = 50;		//���紫������

int main() 
{
	initInputFile();
	//printInputFile();
	
	int i;
	task[0].isScheduled = 1;
	task[TASKNUM-1].isScheduled = 1;
	for(i = 0; i < TIMEDELAYNUM - 3; i++)	//��ֻ���ǽ�ֹʱ��Ϊ 0.6s
	{
		double Td = timedelay[i];	//��ֹʱ��Td
		calESTandLFT(Td);
		
		//scheduleParents(TASKNUM-1);	  //�����һ���ڵ㿪ʼ����	
	}
}

//�ӽڵ�n��ʼ����
void scheduleParents(int n) 
{
	
}

//����ÿ����������翪ʼʱ����������ʱ��
//�������/���ڽڵ�0,12�����������������ƶ�ִ�У�EST��LFT�������£�
void calESTandLFT(double Td)
{
	//���������������ƶ�ִ��
	initExeLoc();
	
	calEST();
	calLFT(Td);
	
	//ÿ������ļ���ʱ��
}

//��������1~12�����翪ʼʱ��
void calEST() 
{
	task[0].earliest_start_time = 0;
	int i, j;
	int *ret;
	double maxEarlistStartTime = 0;
	//����ڵ�1~12�����翪ʼʱ��
	for(i = 1; i < TASKNUM; i++) 
	{
		int len = findParentSet(ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int pre = ret[j];		//�ڵ�i�ĺ�̽ڵ���±�
			double comp_time = calCommpTime(pre);
			double transfer_time = calTransferTime(pre, i);
			double time = task[pre].earliest_start_time + task[pre].comp_time + transfer_time;
			if(time > maxEarlistStartTime)
			{
				maxEarlistStartTime = time;
			}
		}
		task[i].earliest_start_time = maxEarlistStartTime;
	}
}

//��������1~11���������ʱ��
void calLFT(double Td)
{
	int i, j;
	int *ret;
	task[12].lastest_finish_time = Td;
	double minLastestFinishTime = 0;
	//����ڵ�1~12�����翪ʼʱ��
	for(i = TASKNUM - 2; i < 0; i--) 
	{
		int len = findChildSet(ret, i, Td);
		for(j = 0; j < len; j++)
		{
			int succ = ret[j];		//�ڵ�i�ĺ�̽ڵ���±�
			double time = task[succ].lastest_finish_time - task[succ].comp_time - transferData[i][succ] / bandwidth;
			if(time < minLastestFinishTime)
			{
				minLastestFinishTime = time;
			}
		}
		task[i].lastest_finish_time = minLastestFinishTime;
	}
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
	double comp_time;
}

//�ڵ�n�ļ����ܺ�
double calCompEnergy(int n) 
{
	double comp_energy;
	int exeLoc = task[n].exeLoc;
	double comp_time = calCommpTime(n);
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


//����֮��ĸ��ӹ�ϵ
void parentAndChild() 
{
	int i, j;
	for(i = 0; i < TASKNUM; i++)
	{
		for(j = 0; j < TASKNUM; j++) 
		{
			if(transferData[i][j] > 0) 
			{
				task[i].next = task[j];
				task[j].parent = task[i];
			}
		}
	}
}

//����EST��LFT�����ִ��λ��
void initExeLoc() 
{
	int i;
	for(i = 0; i < TASKNUM; i++)
	{
		task[i].exeLoc = 1;
	}
}

//�ҵ�n��ֱ�Ӻ�̽ڵ�
int findChildSet(int *&ret, int n, double Td) 
{
	int i;
	int[] ret;
	int count = 0;
	for(i = 0; i < TASKNUM; i++) 
	{
		if(task[n].next = task[i])
		{
			ret[count++] = i;
		}
	}
	return count;
}

//�ҵ�n��ֱ��ǰ���ڵ�
int findParentSet(int *&ret, int n, double Td)
{
	int i; 
	int count = 0;
	ret = (int *)malloc(sizeof(int));
	for(i = 0; i < TASKNUM; i++) 
	{
		if(task[n].parent = task[i])
		{
			ret[count++] = i;
		}
	}
	return count;
}

//�������翪ʼʱ����������ʱ��
void updateEarliestStartTime() 
{
	
}

updateLastestFinishTime()
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
	//��������
	workload = (double *)malloc(sizeof(double)*TASKNUM);
	fp_file = fopen("workload.dat", "r");
	for(i = 0; i < TASKNUM; i++) 
	{
		fscanf(fp_file, "%lf", &workload[i]);
	}
	//��ֹʱ��
	timedelay = (double *)malloc(sizeof(double)*TIMEDELAYNUM);
	for(i = 0; i < TIMEDELAYNUM; i++)
	{
		fscanf(fp_file, "%lf", &timedelay[i]);
	}
	fclose(fp_file);
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