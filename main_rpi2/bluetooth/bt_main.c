/**
* SPP Server
*
* Description:
* It's a SPP(Serial Port Profile) server which can  
* establish a connection with a unkown bluetooth device 
* and then listen the commands from it.
* 
* Procedure:
* 1. Set the class
* 2. Register with the SDP Server
* 3. Listen for RFCOMM connections
* 4. Process data from the connections
* 5. Execute the control commands
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <bluetooth/sdp.h>
#include <bluetooth/rfcomm.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/bluetooth.h>
#include "spp-sdp-register.h"
#define SHMSIZE 16 			// share memory size

unsigned int cls = 0x1f00;  // Uncategorized class of device
int timeout = 1000;			// millisecond
uint8_t channel = 10;		// channel of SPP server
char *filename = "/home/pi/IODD/data/tracks.json";

key_t keycmd = 5678;  // share memory key
int shmcmdid;		  // share memory id
char *shmcmd;		  // share memory of command

int set_class(unsigned int cls, int timeout);
int rfcomm_listen(uint8_t channel); 
int handle_connection(int rfcommsock);
int listenACK(char *ack, int *rfcommsock);
int musicControl(char message[]);
int sendJSON(int *rfcommsock);
int connectionlost();

int main()
{
	// create the segment
	if ((shmcmdid = shmget(keycmd, SHMSIZE, IPC_CREAT | 0666)) < 0) {
		perror("command shmget ");
		return -1;
	}
	// attach the segment to data space.
	if ((shmcmd = (char*) shmat(shmcmdid, NULL, 0)) == (char *) -1) {
		perror("command shmat");
		return -1;
	}
	*shmcmd = '*';
	*(shmcmd+1) = '\0';


	int rfcommsock; // rfcomm socket

	// set the class of device(CoD), namely service field
	if (set_class(cls, timeout) < 0){
		perror("set_class ");
	}

	// register service records to the SDP server
	if (register_sdp(channel) < 0){
		perror("register_sdp ");
		return -1;
	}

	// Wait the connection form client. If successful, 
	// return the socket descriptor of client
	if ((rfcommsock = rfcomm_listen(channel)) < 0){
		perror("rfcomm_listen ");
		return -1;
	}

 	handle_connection(rfcommsock);
	return 0;
}


int set_class(unsigned int cls, int timeout)
{
	int id; //local bluetooth device id (not mac address)
	int hci_handle; // hci file handle
	bdaddr_t btaddr; //bluetooth mac address
	char bdaddrstr[18]; //using string type instead of bdaddr_t

    // get first available bluetooth device id by passing NULL
	if ((id = hci_get_route(NULL)) < 0){
		return -1;
	}

	// get the mac bdaddr from device id
	if (hci_devba(id, &btaddr) < 0){
		return -1;
	}

	// bdaddr to string
	if (ba2str(&btaddr, bdaddrstr) < 0){
		return -1;
	}

	// get the hci file handle
	if ((hci_handle = hci_open_dev(id)) < 0){
		return -1;
	}

	// set the class of device, the function is in hci_lib.h
	if (hci_write_class_of_dev(hci_handle, cls, timeout) != 0){
		perror("hci_write_class ");
		return -1;
	}

	// close the hci file handle
	hci_close_dev(hci_handle);
	printf("set the dongle %s to class: 0x%06x\n", bdaddrstr, cls);
	return 0;
}


int rfcomm_listen(uint8_t channel)
{
	int localsock;						// socket descriptor for local listener
	int clientsock;						// socket descriptor for remote client
	unsigned int len = sizeof(struct sockaddr_rc);

	struct sockaddr_rc remote = { 0 };		// remote rfcomm socket address
	struct sockaddr_rc local = { 0 };		// local rfcomm socket address
	char remotebdaddr[18];

	// Initialize the local bluetooth socket
	localsock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	local.rc_family = AF_BLUETOOTH;
	local.rc_bdaddr = *BDADDR_ANY;
	local.rc_channel = channel; 
	if (bind(localsock, (struct sockaddr *)&local, sizeof(struct sockaddr_rc)) < 0){
		return -1;
	}

	// put the bound socket listen into listening mode, backlog=1
	if (listen(localsock, 1) < 0){
		return -1;
	}

	printf("accepting connections on channel: %d\n", channel);

	// accept incoming connections, and this function is a blocking call
	clientsock = accept(localsock, (struct sockaddr *)&remote, &len);

	ba2str(&remote.rc_bdaddr, remotebdaddr);
	printf("client address: %s\n", remotebdaddr);

	// make the client socket to nonblocking mode
	if (fcntl(clientsock, F_SETFL, O_NONBLOCK) < 0){
		return -1;
	}

	// return the client sccket descriptor
	return clientsock;
}

int handle_connection(int rfcommsock)
{
	char hello_str[] = "HELLO\r\n";
	char rfcommbuffer[255];
	char *returnmessage; //return message
	int len, flag;

	//As soon as the connection establish, server will send 
	//HELLO to client
	len = send(rfcommsock, hello_str, sizeof(hello_str), 0);
	if(len < 0){
		perror("rfcomm send ");
		return -1;
	}

	// listen to ACK back
	if(listenACK("HELLO", &rfcommsock) > 0)
		printf("get HELLO! start to transfer JSON file.\n");
	else 
		return connectionlost();

	//send JSON file.
	if(sendJSON(&rfcommsock) < 0){
		printf("fail to transfer json file");
		return -1;
	}

	if(listenACK("OK", &rfcommsock) > 0)
		printf("get OK! start to listen commands.\n");
	else
		return connectionlost();

	// start to listen to the commands from client
	while (1){
		// receive data from rfcomm socket, already close blocking 
		len = recv(rfcommsock, rfcommbuffer, 255, 0);

		// EWOULDBLOCK indicates the socket would block if we had a
		// blocking socket.  we'll safely continue if we receive that
		// error. Treat all other errors as fatal
		if (len < 0 && errno != EWOULDBLOCK){
			perror("rfcomm recv ");
			break;
		}
		else if (len > 0){
			rfcommbuffer[len-2] = '\0';  //cut '\r\n'
			//music control, return message to client
			returnmessage = musicControl(rfcommbuffer) ? ("OK\r\n"):("ERROR\r\n");
			send(rfcommsock, returnmessage, strlen(returnmessage), 0);
		}
	}
	close(rfcommsock);    

	printf("client disconnected\n");
	return 0;
}

int listenACK(char *ack, int *rfcommsock)
{
	int len;
	char rfcommbuffer[255];
	// get ack when the client has received the message  
	while (1){
		len = recv(*rfcommsock, rfcommbuffer, 255, 0);
		if (len < 0 && errno != EWOULDBLOCK){
			perror("rfcomm recv ");
			return -1;
		}
		else if (len > 0){
			//cut the '\r' and '\n'
			rfcommbuffer[len-2] = '\0';
			if(!strcmp(rfcommbuffer,ack)){
				return 1;
			}
		}
	}
}

int musicControl(char message[])
{	
	//put commands to player through share memory IPC
	snprintf(shmcmd, SHMSIZE, "%s", message);
	printf("server shmcmd success: %s\n", shmcmd);
	printf("server shmcmd address: %p\n", shmcmd);
	while(1){
		if((!strcmp(shmcmd, "*"))){
			printf("client set *: %p\n", shmcmd);
			return 1;
		}
	}
	// printf("shmcmd fail: %s\n", shmcmd);
	// return 0;
}

int sendJSON(int *rfcommsock){
	FILE* jsonfile = fopen(filename,"r");
	long int len;
	if(jsonfile != NULL){
		//printf("open JSON file successfully!\n");

		// get the size of json file, and put it into the buffer
		fseek(jsonfile, 0, SEEK_END); 	    // set position to the end
		long int size = ftell(jsonfile)+2;  // ftell will return the current position
		rewind(jsonfile); 				    // set position to the begining
		char* content = calloc(size, sizeof(char)); // malloc (size+5)*sizeof(char) with 0
		fread(content, sizeof(char), size, jsonfile); //get the content of json file
		*(content+size+1) = "\r\n"; 		// end with \r\n 

		// send the content
		len = send(*rfcommsock, content, size, 0);
		if(len < 0){
			perror("rfcomm send ");
			return -1;
		}
		fclose(jsonfile);
		return 1;
	}
	else{
		printf("cannot open JSON file: %s\n", filename);
		return -1;
	}
}

int connectionlost(){
	printf("connection lost\n");
	return -1;
}