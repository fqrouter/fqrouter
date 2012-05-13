compile: wcs2-server.c
	$(CC) -Wall -std=c99 $(CFLAGS) -c -o wcs2-server.o $<
	$(CC) -o wcs2-server $(LDFLAGS) wcs2-server.o
clean:
	rm -f *.o *.so wcs2-server
	
