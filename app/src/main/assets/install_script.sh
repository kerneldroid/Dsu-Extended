#!/bin/sh

# unmount sdcard
umount_sd=%UNMOUNT_SD
if [[ $umount_sd == true ]]; then
  SDCARD=$(sm list-volumes | grep -v null | grep public)
  if [[ $SDCARD == "" ]]; then
    echo "已启用卸载 SD 卡选项，但未检测到 SD 卡，跳过.."
    umount_sd=false
  else
    echo "已启用卸载 SD 卡选项，将临时弹出 SD 卡，以避免在 SD 卡上分配 DSU.."
    sm unmount $SDCARD
  fi
fi

# required prop
setprop persist.sys.fflag.override.settings_dynamic_system true

# invoke DSU activity
%ACTION_INSTALL

echo "DSU 安装界面已启动！"

if [[ $umount_sd == true ]]; then
  echo "将在 60 秒后重新挂载 SD 卡.."
  nohup $(sleep 60 && sm mount $SDCARD) >/dev/null 2>&1 &
fi
