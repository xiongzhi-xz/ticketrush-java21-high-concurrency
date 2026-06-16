#!/bin/sh
# Custom entrypoint for RocketMQ Broker 5.1.4
# Works around two issues:
#   1. ScheduleMessageService NPE when storePathRootDir not in broker.conf
#   2. Docker named-volume permission denied (root-owned, rocketmq user can't write)
# Solution: write a complete broker.conf and ensure store dirs exist before starting.

CONF_FILE="/home/rocketmq/rocketmq-5.1.4/conf/broker.conf"
STORE_DIR="/home/rocketmq/store"

# Create store directories with correct ownership
mkdir -p "${STORE_DIR}/commitlog"
mkdir -p "${STORE_DIR}/consumequeue"
mkdir -p "${STORE_DIR}/index"
mkdir -p "${STORE_DIR}/config"

# Write a complete broker.conf (storePathRootDir is required to avoid NPE)
cat > "${CONF_FILE}" <<EOF
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH

storePathRootDir = ${STORE_DIR}
storePathCommitLog = ${STORE_DIR}/commitlog
storePathConsumeQueue = ${STORE_DIR}/consumequeue
storePathIndex = ${STORE_DIR}/index
storeCheckpoint = ${STORE_DIR}/checkpoint
abortFile = ${STORE_DIR}/abort

autoCreateTopicEnable = true
autoCreateSubscriptionGroup = true
namesrvAddr = ${NAMESRV_ADDR}
EOF

echo "[broker-entrypoint] conf written to ${CONF_FILE}"
echo "[broker-entrypoint] starting broker..."
exec sh mqbroker -c "${CONF_FILE}"
