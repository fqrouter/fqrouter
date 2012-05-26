include $(TOPDIR)/rules.mk

PKG_NAME:=fqrouter
PKG_VERSION:=1.0
PKG_RELEASE:=1
PKG_BUILD_DIR:=$(BUILD_DIR)/fqrouter-$(PKG_VERSION)

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./code/* $(PKG_BUILD_DIR)/
endef

define Package/fqrouter
    CATEGORY:=Network
    SUBMENU:=Firewall
    TITLE:=FQ-Router
    URL:=http://fqrouter.github.com/
    MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>
    DEPENDS:=+libnetfilter-queue
endef

define Package/fqrouter/install
	$(INSTALL_DIR) $(1)/usr/sbin
	$(INSTALL_BIN) $(PKG_BUILD_DIR)/fqrouter $(1)/usr/sbin/fqrouter
endef

$(eval $(call BuildPackage,fqrouter))
