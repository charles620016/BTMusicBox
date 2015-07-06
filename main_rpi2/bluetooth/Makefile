CFLAGS = -lbluetooth
CC = gcc

bt_main: shmclienttest
	$(CC) ${CFLAGS} bt_main.c spp-sdp-register.o -o bt_main
shmclienttest:
	$(CC) shmclienttest.c -o shmclienttest

clean:
	rm -f bt_main shmclienttest