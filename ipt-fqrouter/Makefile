include $(TOPDIR)/rules.mk
include $(INCLUDE_DIR)/kernel.mk

PKG_NAME:=ipt-fqrouter
PKG_VERSION:=0.1
PKG_RELEASE:=1
PKG_BUILD_DEPENDS:=iptables

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./code/* $(PKG_BUILD_DIR)/
endef

define Build/Compile
    $(MAKE) -C $(LINUX_DIR) SUBDIRS=$(PKG_BUILD_DIR) LDDINC=$(LINUX_DIR)/include \
        ARCH="$(LINUX_KARCH)" CROSS_COMPILE="$(TARGET_CROSS)" \
        LDFLAGS="$(TARGET_LDFLAGS)" CFLAGS="$(CFLAGS)" modules
endef

define KernelPackage/ipt-fqrouter
    SUBMENU:=Netfilter Extensions
    TITLE:=FQ-Router Kernel Module
    URL:=http://fqrouter.github.com/
    MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>
    DEPENDS:= @!LINUX_2_4 kmod-ipt-core
    FILES:=$(PKG_BUILD_DIR)/xt_fqrouter.$(LINUX_KMOD_SUFFIX)
    AUTOLOAD:=$(call AutoLoad,46,1)
endef

$(eval $(call KernelPackage,ipt-fqrouter))