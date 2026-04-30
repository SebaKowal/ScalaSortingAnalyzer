package monitoring

/**
 * Comprehensive hardware profile containing detailed system information.
 * Used for baseline documentation before performance benchmarking.
 */
case class HardwareProfile(
  // CPU Information
  cpuModel: String,
  cpuVendor: String,
  physicalCores: Int,
  logicalCores: Int,
  currentFrequencyMhz: Double,
  maxFrequencyMhz: Double,
  processorId: String,
  l1CacheKb: Option[String],
  l2CacheKb: Option[String],
  l3CacheKb: Option[String],
  smtEnabled: Option[Boolean],

  // RAM Information
  totalRamGb: Double,
  availableRamGb: Double,
  ramType: String,
  ramSpeedMts: Option[String],

  // OS Information
  osName: String,
  osVersion: String,
  osArch: String,

  // JVM Information
  jvmVendor: String,
  jvmVersion: String,
  jvmRuntime: String,
  jvmXmxMb: Long,
  jvmXmsMb: Long,
  gcType: String,

  // Motherboard/BIOS
  biosManufacturer: Option[String],
  biosModel: Option[String],
  biosVersion: Option[String],

  // Storage
  storageDevices: List[StorageDevice],

  // Timestamp
  capturedAt: String
)

case class StorageDevice(
  model: String,
  interface: String,
  capacityGb: Long
)

