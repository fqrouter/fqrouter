include $(TOPDIR)/rules.mk
include $(INCLUDE_DIR)/kernel.mk

PKG_NAME:=fqrouter
PKG_VERSION:=1.0
PKG_RELEASE:=1

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./code/* $(PKG_BUILD_DIR)/
endef

define Build/Compile
    $(MAKE) -C $(LINUX_DIR) SUBDIRS=$(PKG_BUILD_DIR)/src/iptables-module LDDINC=$(LINUX_DIR)/include \
        ARCH="$(LINUX_KARCH)" CROSS_COMPILE="$(TARGET_CROSS)" \
        LDFLAGS="$(TARGET_LDFLAGS)" CFLAGS="$(CFLAGS)" modules
endef

define KernelPackage/ipt-fqrouter
    SUBMENU:=Netfilter Extensions
    TITLE:=FQ-Router Iptables Module
    URL:=http://fqrouter.github.com/
    MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>
    DEPENDS:= @!LINUX_2_4 kmod-ipt-core
    FILES:=$(PKG_BUILD_DIR)/src/iptables-module/xt_fqrouter.$(LINUX_KMOD_SUFFIX)
    AUTOLOAD:=$(call AutoLoad,46,1)
endef

$(eval $(call KernelPackage,ipt-fqrouter))