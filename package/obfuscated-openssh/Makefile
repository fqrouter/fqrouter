#
# Copyright (C) 2006-2011 OpenWrt.org
#
# This is free software, licensed under the GNU General Public License v2.
# See /LICENSE for more information.
#

include $(TOPDIR)/rules.mk

PKG_NAME:=obfuscated-openssh
PKG_VERSION:=6.1p1
PKG_RELEASE:=1

PKG_SOURCE:=$(PKG_NAME)-$(PKG_VERSION).tar.bz2
PKG_SOURCE_SUBDIR:=$(PKG_NAME)-$(PKG_VERSION)
PKG_SOURCE_PROTO:=git
PKG_SOURCE_URL:=git://github.com/aligo/obfuscated-openssh.git
PKG_SOURCE_VERSION:=master

PKG_BUILD_DEPENDS:=libopenssl

include $(INCLUDE_DIR)/package.mk

define Package/obfuscated-openssh/Default
  SECTION:=net
  CATEGORY:=Network
  DEPENDS:=+libopenssl +zlib +SSP_SUPPORT:libssp
  TITLE:=OpenSSH
  MAINTAINER:=Peter Wagner <tripolar@gmx.at>
  URL:=http://www.openssh.com/
  SUBMENU:=SSH
endef

define Package/obfuscated-openssh-moduli
  $(call Package/obfuscated-openssh/Default)
  DEPENDS+= +openssh-server
  TITLE+= moduli file
endef

define Package/obfuscated-openssh-moduli/description
OpenSSH server moduli file.
endef


define Package/obfuscated-openssh-client
  $(call Package/obfuscated-openssh/Default)
  TITLE+= client
endef

define Package/obfuscated-openssh-client/description
OpenSSH client.
endef

define Package/obfuscated-openssh-client/conffiles
/etc/ssh/ssh_config
endef

define Package/obfuscated-openssh-client-utils
  $(call Package/obfuscated-openssh/Default)
  DEPENDS+= +openssh-client +openssh-keygen
  TITLE+= client utilities
endef

define Package/obfuscated-openssh-client-utils/description
OpenSSH client utilities.
endef

define Package/obfuscated-openssh-keygen
  $(call Package/obfuscated-openssh/Default)
  TITLE+= keygen
endef

define Package/obfuscated-openssh-keygen/description
OpenSSH keygen.
endef

define Package/obfuscated-openssh-server
  $(call Package/obfuscated-openssh/Default)
  DEPENDS+= +openssh-keygen
  TITLE+= server
endef

define Package/obfuscated-openssh-server/description
OpenSSH server.
endef

define Package/obfuscated-openssh-server/conffiles
/etc/ssh/sshd_config
endef

define Package/obfuscated-openssh-sftp-client
  $(call Package/obfuscated-openssh/Default)
  TITLE+= SFTP client
endef

define Package/obfuscated-openssh-sftp-client/description
OpenSSH SFTP client.
endef

define Package/obfuscated-openssh-sftp-server
  $(call Package/obfuscated-openssh/Default)
  TITLE+= SFTP server
endef

define Package/obfuscated-openssh-sftp-server/description
OpenSSH SFTP server.
endef

CONFIGURE_ARGS+= \
	$(DISABLE_NLS) \
	--sysconfdir=/etc/ssh \
	--with-privsep-user=sshd \
	--with-privsep-path=/var/empty \
	--enable-shared \
	--disable-static \
	--disable-debug \
	--disable-strip \
	--disable-etc-default-login \
	--disable-lastlog \
	--disable-utmp \
	--disable-utmpx \
	--disable-wtmp \
	--disable-wtmpx \
	--without-bsd-auth \
	--without-kerberos5 \
	--without-pam \
	--without-x

ifeq ($(CONFIG_OPENSSL_ENGINE),y)
CONFIGURE_ARGS+= \
	--with-ssl-engine
endif

ifneq ($(CONFIG_SSP_SUPPORT),y)
CONFIGURE_ARGS += \
	--without-stackprotect
endif

CONFIGURE_VARS += LD="$(TARGET_CC)"

define Build/Compile
	$(MAKE) -C $(PKG_BUILD_DIR) \
		DESTDIR="$(PKG_INSTALL_DIR)" \
		LIBS="" \
		sftp-server
	$(MAKE) -C $(PKG_BUILD_DIR) \
		DESTDIR="$(PKG_INSTALL_DIR)" \
		STRIP_OPT="" \
		all install
endef

define Package/obfuscated-openssh-moduli/install
	$(INSTALL_DIR) $(1)/etc/ssh
	$(CP) $(PKG_INSTALL_DIR)/etc/ssh/moduli $(1)/etc/ssh/
endef

define Package/obfuscated-openssh-client/install
	$(INSTALL_DIR) $(1)/etc/ssh
	chmod 0700 $(1)/etc/ssh
	$(CP) $(PKG_INSTALL_DIR)/etc/ssh/ssh_config $(1)/etc/ssh/
	$(INSTALL_DIR) $(1)/usr/bin
	$(INSTALL_BIN) $(PKG_INSTALL_DIR)/usr/bin/ssh $(1)/usr/bin/
	$(INSTALL_BIN) $(PKG_INSTALL_DIR)/usr/bin/scp $(1)/usr/bin/
endef

define Package/obfuscated-openssh-client-utils/install
	$(INSTALL_DIR) $(1)/usr/bin
	$(INSTALL_BIN) $(foreach bin,add agent keyscan keysign,$(PKG_BUILD_DIR)/ssh-$(bin)) $(1)/usr/bin/
endef

define Package/obfuscated-openssh-keygen/install
	$(INSTALL_DIR) $(1)/usr/bin
	$(INSTALL_BIN) $(PKG_INSTALL_DIR)/usr/bin/ssh-keygen $(1)/usr/bin/
endef


define Package/obfuscated-openssh-server/install
	$(INSTALL_DIR) $(1)/etc/ssh
	chmod 0700 $(1)/etc/ssh
	$(CP) $(PKG_INSTALL_DIR)/etc/ssh/sshd_config $(1)/etc/ssh/
	$(INSTALL_DIR) $(1)/etc/init.d
	$(INSTALL_BIN) ./files/sshd.init $(1)/etc/init.d/sshd
	$(INSTALL_DIR) $(1)/usr/sbin
	$(INSTALL_BIN) $(PKG_INSTALL_DIR)/usr/sbin/sshd $(1)/usr/sbin/
endef

define Package/obfuscated-openssh-sftp-client/install
	$(INSTALL_DIR) $(1)/usr/bin
	$(INSTALL_BIN) $(PKG_INSTALL_DIR)/usr/bin/sftp $(1)/usr/bin/
endef

define Package/obfuscated-openssh-sftp-server/install
	$(INSTALL_DIR) $(1)/usr/lib
	$(CP) $(PKG_INSTALL_DIR)/usr/lib/sftp-server $(1)/usr/lib/
	$(INSTALL_DIR) $(1)/usr/libexec
	ln -sf ../lib/sftp-server $(1)/usr/libexec/sftp-server
endef

$(eval $(call BuildPackage,obfuscated-openssh-client))
$(eval $(call BuildPackage,obfuscated-openssh-moduli))
$(eval $(call BuildPackage,obfuscated-openssh-client-utils))
$(eval $(call BuildPackage,obfuscated-openssh-keygen))
$(eval $(call BuildPackage,obfuscated-openssh-server))
$(eval $(call BuildPackage,obfuscated-openssh-sftp-client))
$(eval $(call BuildPackage,obfuscated-openssh-sftp-server))
