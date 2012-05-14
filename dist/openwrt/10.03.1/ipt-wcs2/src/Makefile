TARGET=xt_wcs2.o
obj-m:=$(TARGET)
xt_wcs2-objs:=$(OBJS)
TARGETS := $(obj-m:.o=.ko)
clean:
	rm -f .*.cmd *.mod.c *.ko *.o *~ core $(TARGETS)
	rm -fr .tmp_versions

