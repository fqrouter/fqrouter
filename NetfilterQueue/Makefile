#
# Copyright (C) 2007-2008 OpenWrt.org
#
# This is free software, licensed under the GNU General Public License v2.
# See /LICENSE for more information.
#

include $(TOPDIR)/rules.mk

PKG_NAME:=NetfilterQueue
PKG_VERSION:=0.3
PKG_RELEASE:=1

PKG_SOURCE:=$(PKG_NAME)-$(PKG_VERSION).tar.gz
PKG_SOURCE_URL:=http://pypi.python.org/packages/source/N/NetfilterQueue/
PKG_MD5SUM:=4bc4cab885a0326fad3f6d8e2c5c3b09

PKG_BUILD_DEPENDS:=python

include $(INCLUDE_DIR)/package.mk
$(call include_mk, python-package.mk)

define Package/python-netfilter-queue
	SUBMENU:=Python
	SECTION:=lang
	CATEGORY:=Languages
	TITLE:=Python bindings for libnetfilter_queue
	URL:=https://github.com/kti/python-netfilterqueue/
	DEPENDS:=+python-mini +libnetfilter-queue
endef

define PyPackage/python-netfilter-queue/filespec
+|$(PYTHON_PKG_DIR)/netfilterqueue.so
endef

define Build/Compile
	$(if $(Build/Compile/PyMod),,@echo Python packaging code not found.; false)
	$(call Build/Compile/PyMod,., \
		install --prefix="$(PKG_INSTALL_DIR)/usr" \
	)
endef

$(eval $(call PyPackage,python-netfilter-queue))
$(eval $(call BuildPackage,python-netfilter-queue))

