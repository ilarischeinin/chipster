##depends:finish/checksums.bash

## Fix rights 
chown -R -h ${USERNAME}.${GROUPNAME} ${CHIP_PATH}/
if [ -d /mnt/tools/ ]; then
  chown -R ${USERNAME}.${GROUPNAME} /mnt/tools/
fi
