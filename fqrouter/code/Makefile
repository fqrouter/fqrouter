compile: src/fqrouter.c
	$(CC) $(CFLAGS) -c -o fqrouter.o $<
	$(CC) $(LDFLAGS) -o fqrouter -lnetfilter_queue -lnfnetlink fqrouter.o
clean:
	rm -f *.o *.so fqrouter

