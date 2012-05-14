include $(TOPDIR)/rules.mk

PKG_NAME:=iptables-mod-wcs2
PKG_VERSION:=0.1
PKG_RELEASE:=1
PKG_BUILD_DIR:=$(BUILD_DIR)/iptables-mod-wcs2-$(PKG_VERSION)
PKG_BUILD_DEPENDS:=iptables

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./src/* $(PKG_BUILD_DIR)/
endef

define Package/iptables-mod-wcs2
    CATEGORY:=Network
    SUBMENU:=Firewall
    TITLE:=West Chamber Season2 Netfilter Extension (User Land)
    URL:=http://code.google.com/p/xt-wcs2/
    MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>
    DEPENDS:= @!LINUX_2_4 iptables +kmod-ipt-wcs2
endef

define Package/iptables-mod-wcs2/install
	$(INSTALL_DIR) $(1)/usr/lib/iptables
	$(CP) $(PKG_BUILD_DIR)/libxt_wcs2.so $(1)/usr/lib/iptables/ ;
endef

$(eval $(call BuildPackage,iptables-mod-wcs2))
