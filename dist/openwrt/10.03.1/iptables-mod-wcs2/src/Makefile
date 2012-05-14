lib%.so: lib%.o
	$(CC) $(TARGET_LDFLAGS) -shared -o $@ $^;
lib%.o: lib%.c
	$(CC) $(CFLAGS) -fPIC -O2 -Wall -I$(LINUX_DIR)/include -D_INIT=lib$*_init -c -o $@ $<;
compile:libxt_wcs2.so
clean:
	rm -f *.o *.so

