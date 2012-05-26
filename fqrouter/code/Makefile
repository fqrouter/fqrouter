compile: src/fqrouter.c
	$(CC) $(CFLAGS) -Wall -std=c99 -c -o fqrouter.o $<
	$(CC) $(LDFLAGS) -o fqrouter -lnetfilter_queue -lnfnetlink fqrouter.o
clean:
	rm -f *.o *.so fqrouter

