include $(TOPDIR)/rules.mk
include $(INCLUDE_DIR)/kernel.mk

PKG_NAME:=ipt-wcs2
PKG_VERSION:=0.1
PKG_RELEASE:=1
PKG_BUILD_DEPENDS:=iptables

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./src/* $(PKG_BUILD_DIR)/
endef

define Build/Compile
    $(MAKE) -C $(LINUX_DIR) SUBDIRS=$(PKG_BUILD_DIR) LDDINC=$(LINUX_DIR)/include \
        ARCH="$(LINUX_KARCH)" CROSS_COMPILE="$(TARGET_CROSS)" \
        LDFLAGS="$(TARGET_LDFLAGS)" CFLAGS="$(CFLAGS)" modules
endef

define KernelPackage/ipt-wcs2
    SUBMENU:=Netfilter Extensions
    TITLE:=West Chamber Season2 Netfilter Extension (Kernel)
    URL:=http://code.google.com/p/xt-wcs2/
    MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>
    DEPENDS:= @!LINUX_2_4 kmod-ipt-core
    FILES:=$(PKG_BUILD_DIR)/xt_wcs2.$(LINUX_KMOD_SUFFIX)
    AUTOLOAD:=$(call AutoLoad,46,1)
endef

$(eval $(call KernelPackage,ipt-wcs2))
