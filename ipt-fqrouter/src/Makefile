TARGET=xt_fqrouter.o
obj-m:=$(TARGET)
xt_fqrouter-objs:=$(OBJS)
TARGETS := $(obj-m:.o=.ko)
clean:
	rm -f *.ko
