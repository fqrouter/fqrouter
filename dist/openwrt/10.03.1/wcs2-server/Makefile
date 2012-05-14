include $(TOPDIR)/rules.mk

PKG_NAME:=wcs2-server
PKG_VERSION:=0.1
PKG_RELEASE:=1
PKG_BUILD_DIR:=$(BUILD_DIR)/wcs2-server-$(PKG_VERSION)

include $(INCLUDE_DIR)/package.mk

define Build/Prepare
	mkdir -p $(PKG_BUILD_DIR)
	$(CP) ./src/* $(PKG_BUILD_DIR)/
endef

define Package/wcs2-server
  CATEGORY:=Network
  TITLE:=West Chamber Season2 Server
  URL:=http://code.google.com/p/xt-wcs2/
  MAINTAINER:=FQ-Router Development Team <fqrouter-devel@googlegroups.com>  
endef

define Package/wcs2-server/install
	$(INSTALL_DIR) $(1)/usr/sbin
	$(INSTALL_BIN) $(PKG_BUILD_DIR)/wcs2-server $(1)/usr/sbin/wcs2-server
endef

$(eval $(call BuildPackage,wcs2-server))
