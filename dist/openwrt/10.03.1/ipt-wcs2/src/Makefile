TARGET=xt_wcs2.o
obj-m:=$(TARGET)
xt_wcs2-objs:=$(OBJS)
TARGETS := $(obj-m:.o=.ko)
compile:
	$(MAKE) -C $(LINUX_DIR) SUBDIRS=$(PKG_BUILD_DIR) LDDINC=$(LINUX_DIR)/include modules
clean:
	rm -f .*.cmd *.mod.c *.ko *.o *~ core $(TARGETS)
	rm -fr .tmp_versions

