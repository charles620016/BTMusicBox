#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <time.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#define MaxMusic 300
#define SHMSIZE 16
#define CSVLINESIZE 500
#define INITIALVOL -1200

struct controlInfo{
	int caseN;
	int id;
	int vol;
};

struct idPathPair{
	int id;
	char path[CSVLINESIZE];
};

struct nowPlayNo{
	int id;
	int index;
};

struct idPathPair pair[MaxMusic];
struct controlInfo cInfo;
int countMusic = 0;

struct nowPlayNo getMusic(int id, int index, int byID){ //byIDorNot: 1.byID 0.byIndex
	struct nowPlayNo returnMusic;
	if(byID){
		int i;
		for(i=0;i<countMusic;i++){
			if(pair[i].id == id){
				returnMusic.id = id;
				returnMusic.index = i;
				break;
			}
		}
	}else{
		returnMusic.id = pair[index].id;
		returnMusic.index = index;
	}
	return returnMusic;
}

void readMusicPath(){
      FILE* stream = fopen("/home/pi/IODD/data/filedict.csv", "r");

      char line[CSVLINESIZE];
  
      while (fgets(line, CSVLINESIZE, stream) != NULL)
      {
			if(strstr(line, ".mp3") != NULL){
	        	char *pch;
				int id;
				char path[CSVLINESIZE];
				pch = strtok(line, ",");
				id = atoi(pch);
				pch = strtok(NULL, ",");
				*(pch+strlen(pch)-2) = '\0';
				pair[countMusic].id = id;
				strcpy(pair[countMusic].path, pch);
				countMusic++;
          }
      }
      fclose(stream);
}

void readshm(char action[]){
	key_t keycmd = 5678;  // share memory key
	int shmcmdid;		  // share memory id
	char *shmcmd;		  // share memory of command

	char mi[] = "*";
	if ((shmcmdid = shmget(keycmd, SHMSIZE, IPC_CREAT | 0666)) < 0) {
		perror("command shmget ");
		return;
	}
	// attach the segment to data space.
	if ((shmcmd = (char*) shmat(shmcmdid, NULL, 0)) == (char *) -1) {
		perror("command shmat");
		return;
	}
	while(1){
		sleep(2);
		if(strcmp(shmcmd,mi)){  //execute if different
			sleep(1);
			printf("clent shmcmd: %s\n", shmcmd);
			printf("shmcmd address: %p\n", shmcmd);
			snprintf(action, SHMSIZE, "%s", shmcmd);
			break;
		}
	}
	*shmcmd ='\0'; 
	snprintf(shmcmd, SHMSIZE, "%s", mi);	
}

struct controlInfo checkCommand(char *command, int musicId, int vol){
	/* 
	** case1 : kill
	** case2,Xid : play new music with id X
	** up : vol up
	** down : vol down
	** others are 0 (only keep or nothing)
	*/
	int len = strlen(command);
	int id = 0;
	int idUpdated = 0;
	int caseUpdated = 0;
	
	int countVol=0;
	cInfo.caseN = 0;
	cInfo.id = musicId;
	
	int i;
	int j;
	for(i=len-1;i>=0;i--){
		if(caseUpdated == 0 && (i-3)>=0 && command[i] == 'e' && command[i-1] == 's' && command[i-2] == 'a' && command[i-3] == 'c'){
			switch(command[i+1]){
				case '1': // command includes case1
					caseUpdated = 1;
					cInfo.caseN = 1;
					cInfo.id = musicId;
					cInfo.vol = vol;
					return cInfo;
					break;
				case '2': // command includes case2
					caseUpdated = 1;
					cInfo.caseN = 2;
					break;
				default :  
					break;
			}		
		}
		if(idUpdated == 0 && (i-1)>=0 && command[i]=='d' && command[i-1]=='i'){
			for(j=i-3;j>=0;j--){
				if(command[j] == ','){
					break;
				}
			}
			int k;
			for(k=j+1;k<i-1;k++){
				id = (command[k]-48) + id*10;
			}
			cInfo.id = id;
			idUpdated = 1;	
		}	

		if((i-1)>=0 && command[i] == 'p' && command[i-1] == 'u'){ // vol up
			countVol++;
		}else if((i-3)>=0 && command[i] == 'n' && command[i-1] == 'w' && command[i-2] == 'o' && command[i-3] == 'd'){
			countVol--;
		}
	}
	cInfo.vol = vol + countVol*300;
	if(idUpdated == 0){
		struct nowPlayNo now = getMusic(musicId, -1, 1);
		struct nowPlayNo next = getMusic(-1, (now.index+1)%countMusic, 0);
		cInfo.id = next.id;
		cInfo.caseN = 0;
	}
	return cInfo;
	
}

char* parseIDtoMusicPath(int musicId){
	int i;
	for(i=0; i<countMusic; i++){
		if(pair[i].id == musicId){
			return pair[i].path;
		}
	}
	printf("music id not found\n");
	return NULL;
}

void playMusic(int pipeMusicCommand[2], int pipeToChild[2], int musicId, int vol){
	pid_t grandChildPid = (pid_t)NULL;
	char volChar[10];
	int status;
	char buf[1000];
	dup2(pipeToChild[0], STDIN_FILENO);
	
	while(1){
		snprintf(volChar, 10 ,"%d", vol);
		printf("start fork grandchild\n");
		printf("start with vol: %d (%s)", vol, volChar);
		grandChildPid = fork();
		if(grandChildPid == 0){
			dup2(pipeMusicCommand[0], STDIN_FILENO);

			printf("musicID : %d\n", musicId);
			printf("music path : %s\n", parseIDtoMusicPath(musicId));
			execl("/usr/bin/omxplayer", "/usr/bin/omxplayer", parseIDtoMusicPath(musicId), "--vol", volChar, (char*) NULL);
			printf("grandchild die\n");
			exit(1);
		}
		printf("start wait grandchild\n");
		waitpid((pid_t)grandChildPid, &status, 0);
		printf("finish wait grandchild\n");
		write(pipeToChild[1], "keep", sizeof("keep"));
		read(pipeToChild[0], buf, sizeof(buf));
		printf("(%s)\n", buf);
		struct controlInfo checkResult = checkCommand(buf, musicId, vol);
		printf("receive check result\n");
		if(checkResult.caseN == 1){
			break;
		}
		else if(checkResult.caseN == 2){
			printf("case2\n");
			musicId = checkResult.id;
			vol = checkResult.vol;
		}
		else if(checkResult.caseN == 0){
			printf("case0\n");
			musicId = checkResult.id;
			vol = checkResult.vol;
		}
		snprintf(volChar, 10 ,"%d", vol);
		printf("die with vol: %d (%s)", vol, volChar);
	}
	exit(1);
}

void main(){
	
	pid_t childPid = (pid_t)NULL;
	int pipeMusicCommand[2];
	int pipeToChild[2];
	// status variables
	int status;
	int statePlaying = 0; // 0: INITIAL 1: STOP, 2: PAUSE, 3: PLAYING
	int nowVol = INITIALVOL; // volumn , default : INITIALVOL
	struct nowPlayNo nowPlay; // music playing right now

	readMusicPath();
	pipe(pipeMusicCommand);
	pipe(pipeToChild);

	char command[10];

	//int flag = 0;
	while(1){
		//scanf("%s", command);
		char action[SHMSIZE];
		printf("=====================\n");
		readshm(action);
		printf("start action\n");
		printf("%s with statePlaying %d\n", action, statePlaying);
		if(strlen(action)>3 && strstr(action, "ID_")==action){
			int id = atoi(action+3); //cast id numer to integer 
			printf("Play : Track id = %d\n", id);
			nowPlay = getMusic(id,-1,1);
						
			if(statePlaying == 0){
				statePlaying = 3;
				printf("assign statePlaying %d\n", statePlaying);
				childPid = fork();
				if(childPid == 0){
					printf("start play music\n");
					playMusic(pipeMusicCommand, pipeToChild, nowPlay.id, nowVol);
			
				}
			}else if(statePlaying == 1 || statePlaying == 2 || statePlaying == 3){
				statePlaying = 3;

				char msgToChild[100];
				snprintf(msgToChild, 100, "case2,%did", nowPlay.id);
				printf("nowPlayID: %d\n", nowPlay.id);
				printf("send msg to child: %s\n",msgToChild);
				write(pipeToChild[1], msgToChild, strlen(msgToChild)); // if id = 5, then send case2,5id
				
				command[0] = 'q';
				write(pipeMusicCommand[1], command, 1); // finish the existing music to start new music
			}
			else{
				printf("undefined status\n");
			}
		}
		else if(!strcmp(action,"PLAY")){
			printf("Execute : PLAY the track. \n");
			if(statePlaying == 0){
				// do nothing: should not be happened 
			}else if(statePlaying == 1){
				// start music from begining
				statePlaying = 3;
				char msgToChild[100];
				printf("nowPlayID: %d\n", nowPlay.id);
				snprintf(msgToChild, 100, "case2,%did", nowPlay.id);
				write(pipeToChild[1], msgToChild, strlen(msgToChild)); // if id = 5, then send case2,5id
				
				command[0] = 'q';
				write(pipeMusicCommand[1], command, 1); // finish the existing music to start new music
			}else if(statePlaying == 2){
				// resume from pause
				statePlaying = 3;

				command[0] = 'p';
				write(pipeMusicCommand[1], command, 1);
			}else if(statePlaying == 3){
				// do nothing: KEEP PLAYING
				printf("KEEP  PLAYING\n");
			}
		}
		else if(!strcmp(action,"STOP")){
			printf("Execute : STOP the track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1 || statePlaying == 2){
				statePlaying = 1;
				// do nothing
			}else if(statePlaying == 3){
				statePlaying = 1;
				command[0] = 'p';
				write(pipeMusicCommand[1], command, 1); // pause the music to simulate stop
			}
		}
		else if(!strcmp(action,"PAUSE")){
			printf("Execute : SUSPEND the track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1){
				// do nothing: STOP
			}else if(statePlaying == 2){
				// do nothing: KEEP PAUSE
				printf("(action:pause, stateplaying:pause)\n");
			}else if(statePlaying == 3){
				statePlaying = 2;
				command[0] = 'p';
				write(pipeMusicCommand[1], command, 1); // pause the music
			}
		}
		else if(!strcmp(action,"NEXT")){
			printf("Execute : Switch to NEXT track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1 || statePlaying == 2){
				// get new ID and stop at 0:00
				nowPlay = getMusic(-1, (nowPlay.index+1)%countMusic, 0);
				statePlaying = 1;
			}else if(statePlaying == 3){
				nowPlay = getMusic(-1, (nowPlay.index+1)%countMusic, 0);
				statePlaying = 3;
				
				char msgToChild[100];
				snprintf(msgToChild, 100, "case2,%did", nowPlay.id);
				printf("nowPlayID: %d\n", nowPlay.id);
				printf("send msg to child: %s\n",msgToChild);
				write(pipeToChild[1], msgToChild, strlen(msgToChild)); // if id = 5, then send case2,5id
				sleep(1);

				command[0] = 'q';
				write(pipeMusicCommand[1], command, 1); // finish the existing music to start new music
			}
		}
		else if(!strcmp(action,"PREVIOUS")){
			printf("Execute : Switch to PREVIOUS track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1 || statePlaying == 2){
				// get new ID and stop at 0:00
				nowPlay = getMusic(-1, (nowPlay.index-1+countMusic)%countMusic, 0);		
				statePlaying = 1;
			}else if(statePlaying == 3){
				nowPlay = getMusic(-1, (nowPlay.index-1+countMusic)%countMusic, 0);
				statePlaying = 3;

				char msgToChild[100];
				snprintf(msgToChild, 100, "case2,%did", nowPlay.id);
				printf("nowPlayID: %d\n", nowPlay.id);
				printf("send msg to child: %s\n",msgToChild);
				write(pipeToChild[1], msgToChild, strlen(msgToChild)); // if id = 5, then send case2,5id
				sleep(1);

				command[0] = 'q';
				write(pipeMusicCommand[1], command, 1); // finish the existing music to start new music	
			}
		}
		else if(!strcmp(action,"FORWARD")){
			printf("Execute : Fast FORWARD the track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1){
				// do nothing: STOP
			}else if(statePlaying == 2){
				// KEEP PAUSE
				statePlaying = 2;
				// forward 30 secs
				command[0] = '^[[C';
				write(pipeMusicCommand[1], command, 1); // pass right arrow
				sleep(2);

				char buf[100];
				read(pipeMusicCommand[0], buf, sizeof(buf));
				printf("(%s)",buf);
			}else if(statePlaying == 3){
				// KEEP PLAYING
				statePlaying = 3;
				// forward 30 secs
				command[0] = '^[[C';
				write(pipeMusicCommand[1], command, 1); // pass right arrow
			}
		}
		else if(!strcmp(action,"REWIND")){
			printf("Execute : REWIND the track.\n");
			if(statePlaying == 0){
				// do nothing: should not be happened
			}else if(statePlaying == 1){
				// do nothing: STOP
			}else if(statePlaying == 2){
				// KEEP PAUSE
				statePlaying = 2;
				// rewind 30 secs
				//command[0] = '^[[D';
				write(pipeMusicCommand[1], 37, 1); // pass left arrow
			}else if(statePlaying == 3){
				// KEEP PLAYING
				statePlaying = 3;
				// rewind 30 secs
				//command[0] = '^[[D';
				write(pipeMusicCommand[1], 37, 1); // pass left arrow
			}
		}
		else if(!strcmp(action,"VOLUP")){
			nowVol += 300;
			write(pipeToChild[1], "up", strlen("up")); // pass "up" to child
			command[0] = '+';
			write(pipeMusicCommand[1], command, 1); // pass +
		}
		else if(!strcmp(action,"VOLDW")){
			nowVol -= 300;
			write(pipeToChild[1], "down", strlen("down")); // pass "down" to child
			command[0] = '-';
			write(pipeMusicCommand[1], command, 1); // pass -
		}
		else if(!strcmp(action,"NOW")){
			printf("statePlaying : %d\n", statePlaying);
			printf("nowVol : %d\n", nowVol);
			printf("nowPlay id : %d\n", nowPlay.id);
			printf("nowPlay index : %d\n", nowPlay.index);
		}
		else if(!strcmp(action,"ERROR")){
			write(pipeToChild[1], "case1", strlen("case1")); // write message to the process
			command[0] = 'q';
			write(pipeMusicCommand[1], command, 1); // pass q
			break;
		}
		else{
			printf("Execute : \"%s\" is wrong message.\n", action);
		}

		//if(flag==0){
		//	write(pipeToChild[1], "keep", strlen("keep")); // write message to the process
		//	flag++;
		//}	
	}
	waitpid((pid_t)childPid, &status, 0);
	kill(0, SIGTERM);
}

