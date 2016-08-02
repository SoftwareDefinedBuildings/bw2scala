package edu.berkeley.cs.sdb.bw2

object POAllocations {
    /*
     * Double (1.0.2.0/32): Double
     * This payload is an 8 byte long IEEE 754 double floating point value
     * encoded in little endian. This should only be used if the semantic
     * meaning is obvious in the context, otherwise a PID with a more specific
     * semantic meaning should be used.
     */
    val PONumDouble = 16777728
    val PODFMaskDouble = "1.0.2.0/32"
    val PODFDouble = (1, 0, 2, 0)
    val POMaskDouble = 32

    /*
     * BWMessage (1.0.1.1/32): Packed Bosswave Message
     * This object contains an entire signed and encoded bosswave message
     */
    val PONumBWMessage = 16777473
    val PODFMaskBWMessage = "1.0.1.1/32"
    val PODFBWMessage = (1, 0, 1, 1)
    val POMaskBWMessage = 32

    /*
     * SpawnpointSvcHb (2.0.2.2/32): SpawnPoint Service Heartbeat
     * A heartbeat from spawnpoint about a currently running service. It is a
     * msgpack dictionary that contains the keys "SpawnpointURI", "Name",
     * "Time", "MemAlloc", and "CpuShares".
     */
    val PONumSpawnpointSvcHb = 33554946
    val PODFMaskSpawnpointSvcHb = "2.0.2.2/32"
    val PODFSpawnpointSvcHb = (2, 0, 2, 2)
    val POMaskSpawnpointSvcHb = 32

    /*
     * Wavelet (1.0.6.1/32): Wavelet binary
     * This object contains a BOSSWAVE Wavelet
     */
    val PONumWavelet = 16778753
    val PODFMaskWavelet = "1.0.6.1/32"
    val PODFWavelet = (1, 0, 6, 1)
    val POMaskWavelet = 32

    /*
     * SpawnpointHeartbeat (2.0.2.1/32): SpawnPoint heartbeat
     * A heartbeat message from spawnpoint. It is a msgpack dictionary that
     * contains the keys "Alias", "Time", "TotalMem", "TotalCpuShares",
     * "AvailableMem", and "AvailableCpuShares".
     */
    val PONumSpawnpointHeartbeat = 33554945
    val PODFMaskSpawnpointHeartbeat = "2.0.2.1/32"
    val PODFSpawnpointHeartbeat = (2, 0, 2, 1)
    val POMaskSpawnpointHeartbeat = 32

    /*
     * ROPermissionDChain (0.0.0.18/32): Permission DChain
     * A permission dchain
     */
    val PONumROPermissionDChain = 18
    val PODFMaskROPermissionDChain = "0.0.0.18/32"
    val PODFROPermissionDChain = (0, 0, 0, 18)
    val POMaskROPermissionDChain = 32

    /*
     * GilesTimeseriesResponse (2.0.8.4/32): Giles Timeseries Response
     * A dictionary containing timeseries results for a query. Has 2 keys: -
     * Nonce: the uint32 number corresponding to the query nonce that generated
     * this timeseries response - Data: list of GilesTimeseries (2.0.8.5)
     * objects - Stats: list of GilesStatistics (2.0.8.6) objects
     */
    val PONumGilesTimeseriesResponse = 33556484
    val PODFMaskGilesTimeseriesResponse = "2.0.8.4/32"
    val PODFGilesTimeseriesResponse = (2, 0, 8, 4)
    val POMaskGilesTimeseriesResponse = 32

    /*
     * ROEntityWKey (0.0.0.50/32): Entity with signing key
     * An entity with signing key
     */
    val PONumROEntityWKey = 50
    val PODFMaskROEntityWKey = "0.0.0.50/32"
    val PODFROEntityWKey = (0, 0, 0, 50)
    val POMaskROEntityWKey = 32

    /*
     * ROAccessDOT (0.0.0.32/32): Access DOT
     * An access DOT
     */
    val PONumROAccessDOT = 32
    val PODFMaskROAccessDOT = "0.0.0.32/32"
    val PODFROAccessDOT = (0, 0, 0, 32)
    val POMaskROAccessDOT = 32

    /*
     * ROOriginVK (0.0.0.49/32): Origin verifying key
     * The origin VK of a message that does not contain a PAC
     */
    val PONumROOriginVK = 49
    val PODFMaskROOriginVK = "0.0.0.49/32"
    val PODFROOriginVK = (0, 0, 0, 49)
    val POMaskROOriginVK = 32

    /*
     * GilesKeyValueMetadata (2.0.8.3/32): Giles Key Value Metadata
     * A dictionary containing metadata results for a single stream. Has 2 keys:
     * - UUID: string identifying the stream - Metadata: a map of keys->values
     * of metadata
     */
    val PONumGilesKeyValueMetadata = 33556483
    val PODFMaskGilesKeyValueMetadata = "2.0.8.3/32"
    val PODFGilesKeyValueMetadata = (2, 0, 8, 3)
    val POMaskGilesKeyValueMetadata = 32

    /*
     * Binary (0.0.0.0/4): Binary protocols
     * This is a superclass for classes that are generally unreadable in their
     * plain form and require translation.
     */
    val PONumBinary = 0
    val PODFMaskBinary = "0.0.0.0/4"
    val PODFBinary = (0, 0, 0, 0)
    val POMaskBinary = 4

    /*
     * FMDIntentString (64.0.1.1/32): FMD Intent String
     * A plain string used as an intent for the follow-me display service.
     */
    val PONumFMDIntentString = 1073742081
    val PODFMaskFMDIntentString = "64.0.1.1/32"
    val PODFFMDIntentString = (64, 0, 1, 1)
    val POMaskFMDIntentString = 32

    /*
     * MsgPack (2.0.0.0/8): MsgPack
     * This class is for schemas that are represented in MsgPack
     */
    val PONumMsgPack = 33554432
    val PODFMaskMsgPack = "2.0.0.0/8"
    val PODFMsgPack = (2, 0, 0, 0)
    val POMaskMsgPack = 8

    /*
     * ROAccessDChain (0.0.0.2/32): Access DChain
     * An access dchain
     */
    val PONumROAccessDChain = 2
    val PODFMaskROAccessDChain = "0.0.0.2/32"
    val PODFROAccessDChain = (0, 0, 0, 2)
    val POMaskROAccessDChain = 32

    /*
     * HamiltonBase (2.0.4.0/24): Hamilton Messages
     * This is the base class for messages used with the Hamilton motes. The
     * only key guaranteed is "#" that contains a uint16 representation of the
     * serial of the mote the message is destined for or originated from.
     */
    val PONumHamiltonBase = 33555456
    val PODFMaskHamiltonBase = "2.0.4.0/24"
    val PODFHamiltonBase = (2, 0, 4, 0)
    val POMaskHamiltonBase = 24

    /*
     * YAML (67.0.0.0/8): YAML
     * This class is for schemas that are represented in YAML
     */
    val PONumYAML = 1124073472
    val PODFMaskYAML = "67.0.0.0/8"
    val PODFYAML = (67, 0, 0, 0)
    val POMaskYAML = 8

    /*
     * LogDict (2.0.1.0/24): LogDict
     * This class is for log messages encoded in msgpack
     */
    val PONumLogDict = 33554688
    val PODFMaskLogDict = "2.0.1.0/24"
    val PODFLogDict = (2, 0, 1, 0)
    val POMaskLogDict = 24

    /*
     * RORevocation (0.0.0.80/32): Revocation
     * A revocation for an Entity or a DOT
     */
    val PONumRORevocation = 80
    val PODFMaskRORevocation = "0.0.0.80/32"
    val PODFRORevocation = (0, 0, 0, 80)
    val POMaskRORevocation = 32

    /*
     * JSON (65.0.0.0/8): JSON
     * This class is for schemas that are represented in JSON
     */
    val PONumJSON = 1090519040
    val PODFMaskJSON = "65.0.0.0/8"
    val PODFJSON = (65, 0, 0, 0)
    val POMaskJSON = 8

    /*
     * InterfaceDescriptor (2.0.6.1/32): InterfaceDescriptor
     * This object is used to describe an interface. It contains "uri",
     * "iface","svc","namespace" "prefix" and "metadata" keys.
     */
    val PONumInterfaceDescriptor = 33555969
    val PODFMaskInterfaceDescriptor = "2.0.6.1/32"
    val PODFInterfaceDescriptor = (2, 0, 6, 1)
    val POMaskInterfaceDescriptor = 32

    /*
     * GilesKeyValueQuery (2.0.8.1/32): Giles Key Value Query
     * Expresses a query to a Giles instance. Expects 2 keys: - Query: A Giles
     * query string following syntax at
     * https://gtfierro.github.io/giles2/interface/#querylang - Nonce: a unique
     * uint32 number for identifying the results of this query
     */
    val PONumGilesKeyValueQuery = 33556481
    val PODFMaskGilesKeyValueQuery = "2.0.8.1/32"
    val PODFGilesKeyValueQuery = (2, 0, 8, 1)
    val POMaskGilesKeyValueQuery = 32

    /*
     * GilesMetadataResponse (2.0.8.2/32): Giles Metadata Response
     * Dictionary containing metadata results for a query. Has 2 keys: - Nonce:
     * the uint32 number corresponding to the query nonce that generated this
     * metadata response - Data: list of GilesKeyValueMetadata (2.0.8.3) objects
     */
    val PONumGilesMetadataResponse = 33556482
    val PODFMaskGilesMetadataResponse = "2.0.8.2/32"
    val PODFGilesMetadataResponse = (2, 0, 8, 2)
    val POMaskGilesMetadataResponse = 32

    /*
     * ROEntity (0.0.0.48/32): Entity
     * An entity
     */
    val PONumROEntity = 48
    val PODFMaskROEntity = "0.0.0.48/32"
    val PODFROEntity = (0, 0, 0, 48)
    val POMaskROEntity = 32

    /*
     * HSBLightMessage (2.0.5.1/32): HSBLight Message
     * This object may contain "hue", "saturation", "brightness" fields with a
     * float from 0 to 1. It may also contain an "state" key with a boolean.
     * Omitting fields leaves them at their previous state.
     */
    val PONumHSBLightMessage = 33555713
    val PODFMaskHSBLightMessage = "2.0.5.1/32"
    val PODFHSBLightMessage = (2, 0, 5, 1)
    val POMaskHSBLightMessage = 32

    /*
     * SMetadata (2.0.3.1/32): Simple Metadata entry
     * This contains a simple "val" string and "ts" int64 metadata entry. The
     * key is determined by the URI. Other information MAY be present in the
     * msgpacked object. The timestamp is used for merging metadata entries.
     */
    val PONumSMetadata = 33555201
    val PODFMaskSMetadata = "2.0.3.1/32"
    val PODFSMetadata = (2, 0, 3, 1)
    val POMaskSMetadata = 32

    /*
     * TimeseriesReading (2.0.9.16/28): Timeseries Reading
     * Map with at least these keys: - UUID: string UUID uniquely identifying
     * this timeseries - Time: int64 timestamp, UTC nanoseconds - Value: float64
     * value
     */
    val PONumTimeseriesReading = 33556752
    val PODFMaskTimeseriesReading = "2.0.9.16/28"
    val PODFTimeseriesReading = (2, 0, 9, 16)
    val POMaskTimeseriesReading = 28

    /*
     * ROPermissionDOT (0.0.0.33/32): Permission DOT
     * A permission DOT
     */
    val PONumROPermissionDOT = 33
    val PODFMaskROPermissionDOT = "0.0.0.33/32"
    val PODFROPermissionDOT = (0, 0, 0, 33)
    val POMaskROPermissionDOT = 32

    /*
     * BWRoutingObject (0.0.0.0/24): Bosswave Routing Object
     * This class and schema block is reserved for bosswave routing objects
     * represented using the full PID.
     */
    val PONumBWRoutingObject = 0
    val PODFMaskBWRoutingObject = "0.0.0.0/24"
    val PODFBWRoutingObject = (0, 0, 0, 0)
    val POMaskBWRoutingObject = 24

    /*
     * BW2Chat_ChatMessage (2.0.7.2/32): BW2Chat_ChatMessage
     * A textual message to be sent to all members of a chatroom. This is a
     * dictionary with three keys: 'Room', the name of the room to publish to
     * (this is actually implicit in the publishing), 'From', the alias you are
     * using for the chatroom, and 'Message', the actual string to be displayed
     * to all users in the room.
     */
    val PONumBW2Chat_ChatMessage = 33556226
    val PODFMaskBW2Chat_ChatMessage = "2.0.7.2/32"
    val PODFBW2Chat_ChatMessage = (2, 0, 7, 2)
    val POMaskBW2Chat_ChatMessage = 32

    /*
     * BW2Chat_CreateRoomMessage (2.0.7.1/32): BW2Chat_CreateRoomMessage
     * A dictionary with a single key "Name" indicating the room to be created.
     * This will likely be deprecated.
     */
    val PONumBW2Chat_CreateRoomMessage = 33556225
    val PODFMaskBW2Chat_CreateRoomMessage = "2.0.7.1/32"
    val PODFBW2Chat_CreateRoomMessage = (2, 0, 7, 1)
    val POMaskBW2Chat_CreateRoomMessage = 32

    /*
     * CapnP (3.0.0.0/8): Captain Proto
     * This class is for captain proto interfaces. Schemas below this should
     * include the key "schema" with a url to their .capnp file
     */
    val PONumCapnP = 50331648
    val PODFMaskCapnP = "3.0.0.0/8"
    val PODFCapnP = (3, 0, 0, 0)
    val POMaskCapnP = 8

    /*
     * ROAccessDChainHash (0.0.0.1/32): Access DChain hash
     * An access dchain hash
     */
    val PONumROAccessDChainHash = 1
    val PODFMaskROAccessDChainHash = "0.0.0.1/32"
    val PODFROAccessDChainHash = (0, 0, 0, 1)
    val POMaskROAccessDChainHash = 32

    /*
     * ROPermissionDChainHash (0.0.0.17/32): Permission DChain hash
     * A permission dchain hash
     */
    val PONumROPermissionDChainHash = 17
    val PODFMaskROPermissionDChainHash = "0.0.0.17/32"
    val PODFROPermissionDChainHash = (0, 0, 0, 17)
    val POMaskROPermissionDChainHash = 32

    /*
     * AccountBalance (64.0.1.2/32): Account balance
     * A comma seperated representation of an account and its balance as
     * addr,decimal,human_readable. For example 0x49b1d037c33fdaad75d2532cd373fb
     * 5db87cc94c,57203431159181996982272,57203.4311 Ether  . Be careful in that
     * the decimal representation will frequently be bigger than an int64.
     */
    val PONumAccountBalance = 1073742082
    val PODFMaskAccountBalance = "64.0.1.2/32"
    val PODFAccountBalance = (64, 0, 1, 2)
    val POMaskAccountBalance = 32

    /*
     * SpawnpointConfig (67.0.2.0/32): SpawnPoint config
     * A configuration file for SpawnPoint (github.com/immesys/spawnpoint)
     */
    val PONumSpawnpointConfig = 1124073984
    val PODFMaskSpawnpointConfig = "67.0.2.0/32"
    val PODFSpawnpointConfig = (67, 0, 2, 0)
    val POMaskSpawnpointConfig = 32

    /*
     * BW2Chat_LeaveRoom (2.0.7.4/32): BW2Chat_LeaveRoom
     * Notify users in the chatroom that you have left. Dictionary with a single
     * key "Alias" that has a value of your nickname
     */
    val PONumBW2Chat_LeaveRoom = 33556228
    val PODFMaskBW2Chat_LeaveRoom = "2.0.7.4/32"
    val PODFBW2Chat_LeaveRoom = (2, 0, 7, 4)
    val POMaskBW2Chat_LeaveRoom = 32

    /*
     * GilesTimeseries (2.0.8.5/32): Giles Timeseries
     * A dictionary containing timeseries results for a single stream. has 3
     * keys: - UUID: string identifying the stream - Times: list of uint64
     * timestamps - Values: list of float64 values Times and Values will line
     * up, e.g. index i of Times corresponds to index i of values
     */
    val PONumGilesTimeseries = 33556485
    val PODFMaskGilesTimeseries = "2.0.8.5/32"
    val PODFGilesTimeseries = (2, 0, 8, 5)
    val POMaskGilesTimeseries = 32

    /*
     * GilesArchiveRequest (2.0.8.0/32): Giles Archive Request
     * A MsgPack dictionary with the following keys: - URI (optional): the URI
     * to subscribe to for data - PO (required): which PO object type to extract
     * from messages on the URI - UUID (optional): the UUID to use, else it is
     * consistently autogenerated. - Value (required): ObjectBuilder expression
     * for how to extract the value - Time (optional): ObjectBuilder expression
     * for how to extract any timestamp - TimeParse (optional): How to parse
     * that timestamp - MetadataURI (optional): a base URI to scan for metadata
     * (expands to uri/!meta/+) - MetadataBlock (optional): URI containing a
     * key-value structure of metadata - MetadataExpr (optional): ObjectBuilder
     * expression to search for a key-value structure in the current message for
     * metadata ObjectBuilder expressions are documented at:
     * https://github.com/gtfierro/giles2/tree/master/objectbuilder
     */
    val PONumGilesArchiveRequest = 33556480
    val PODFMaskGilesArchiveRequest = "2.0.8.0/32"
    val PODFGilesArchiveRequest = (2, 0, 8, 0)
    val POMaskGilesArchiveRequest = 32

    /*
     * ROExpiry (0.0.0.64/32): Expiry
     * Sets an expiry for the message
     */
    val PONumROExpiry = 64
    val PODFMaskROExpiry = "0.0.0.64/32"
    val PODFROExpiry = (0, 0, 0, 64)
    val POMaskROExpiry = 32

    /*
     * Giles_Messages (2.0.8.0/24): Giles Messages
     * Messages for communicating with a Giles archiver
     */
    val PONumGiles_Messages = 33556480
    val PODFMaskGiles_Messages = "2.0.8.0/24"
    val PODFGiles_Messages = (2, 0, 8, 0)
    val POMaskGiles_Messages = 24

    /*
     * BinaryActuation (1.0.1.0/32): Binary actuation
     * This payload object is one byte long, 0x00 for off, 0x01 for on.
     */
    val PONumBinaryActuation = 16777472
    val PODFMaskBinaryActuation = "1.0.1.0/32"
    val PODFBinaryActuation = (1, 0, 1, 0)
    val POMaskBinaryActuation = 32

    /*
     * RODRVK (0.0.0.51/32): Designated router verifying key
     * a 32 byte designated router verifying key
     */
    val PONumRODRVK = 51
    val PODFMaskRODRVK = "0.0.0.51/32"
    val PODFRODRVK = (0, 0, 0, 51)
    val POMaskRODRVK = 32

    /*
     * BW2ChatMessages (2.0.7.0/24): BW2ChatMessages
     * These are MsgPack dictionaries sent for the BW2Chat program
     * (https://github.com/gtfierro/bw2chat)
     */
    val PONumBW2ChatMessages = 33556224
    val PODFMaskBW2ChatMessages = "2.0.7.0/24"
    val PODFBW2ChatMessages = (2, 0, 7, 0)
    val POMaskBW2ChatMessages = 24

    /*
     * Blob (1.0.0.0/8): Blob
     * This is a class for schemas that do not use a public encoding format. In
     * general it should be avoided. Schemas below this should include the key
     * "readme" with a url to a description of the schema that is sufficiently
     * detailed to allow for a developer to reverse engineer the protocol if
     * required.
     */
    val PONumBlob = 16777216
    val PODFMaskBlob = "1.0.0.0/8"
    val PODFBlob = (1, 0, 0, 0)
    val POMaskBlob = 8

    /*
     * UniqueObjectStream (2.0.9.0/24): Unique Object Stream
     * An object that is part of a (possibly ordered) stream, identified by
     * UUID. It must contain at least a UUID key uniquely identifying the
     * collection
     */
    val PONumUniqueObjectStream = 33556736
    val PODFMaskUniqueObjectStream = "2.0.9.0/24"
    val PODFUniqueObjectStream = (2, 0, 9, 0)
    val POMaskUniqueObjectStream = 24

    /*
     * SpawnpointLog (2.0.2.0/32): Spawnpoint stdout
     * This contains stdout data from a spawnpoint container. It is a msgpacked
     * dictionary that contains a "service" key, a "time" key (unix nano
     * timestamp) and a "contents" key and a "spalias" key.
     */
    val PONumSpawnpointLog = 33554944
    val PODFMaskSpawnpointLog = "2.0.2.0/32"
    val PODFSpawnpointLog = (2, 0, 2, 0)
    val POMaskSpawnpointLog = 32

    /*
     * BW2Chat_JoinRoom (2.0.7.3/32): BW2Chat_JoinRoom
     * Notify users in the chatroom that you have joined. Dictionary with a
     * single key "Alias" that has a value of your nickname
     */
    val PONumBW2Chat_JoinRoom = 33556227
    val PODFMaskBW2Chat_JoinRoom = "2.0.7.3/32"
    val PODFBW2Chat_JoinRoom = (2, 0, 7, 3)
    val POMaskBW2Chat_JoinRoom = 32

    /*
     * XML (66.0.0.0/8): XML
     * This class is for schemas that are represented in XML
     */
    val PONumXML = 1107296256
    val PODFMaskXML = "66.0.0.0/8"
    val PODFXML = (66, 0, 0, 0)
    val POMaskXML = 8

    /*
     * HamiltonTelemetry (2.0.4.64/26): Hamilton Telemetry
     * This object contains a "#" field for the serial number, as well as
     * possibly containing an "A" field with a list of X, Y, and Z accelerometer
     * values. A "T" field containing the temperature as an integer in degrees C
     * multiplied by 10000, and an "L" field containing the illumination in Lux.
     */
    val PONumHamiltonTelemetry = 33555520
    val PODFMaskHamiltonTelemetry = "2.0.4.64/26"
    val PODFHamiltonTelemetry = (2, 0, 4, 64)
    val POMaskHamiltonTelemetry = 26

    /*
     * Text (64.0.0.0/4): Human readable text
     * This is a superclass for classes that are moderately understandable if
     * they are read directly in their binary form. Generally these are
     * protocols that were designed specifically to be human readable.
     */
    val PONumText = 1073741824
    val PODFMaskText = "64.0.0.0/4"
    val PODFText = (64, 0, 0, 0)
    val POMaskText = 4

    /*
     * GilesStatistics (2.0.8.6/32): Giles Statistics
     * A dictionary containing timeseries results for a single stream. has 3
     * keys: - UUID: string identifying the stream - Times: list of uint64
     * timestamps - Count: list of uint64 values - Min: list of float64 values -
     * Mean: list of float64 values - Max: list of float64 values All fields
     * will line up, e.g. index i of Times corresponds to index i of Count
     */
    val PONumGilesStatistics = 33556486
    val PODFMaskGilesStatistics = "2.0.8.6/32"
    val PODFGilesStatistics = (2, 0, 8, 6)
    val POMaskGilesStatistics = 32

    /*
     * TSTaggedMP (2.0.3.0/24): TSTaggedMP
     * This superclass describes "ts"->int64 tagged msgpack objects. The
     * timestamp is used for merging entries and determining which is later and
     * should be the final value.
     */
    val PONumTSTaggedMP = 33555200
    val PODFMaskTSTaggedMP = "2.0.3.0/24"
    val PODFTSTaggedMP = (2, 0, 3, 0)
    val POMaskTSTaggedMP = 24

    /*
     * String (64.0.1.0/32): String
     * A plain string with no rigid semantic meaning. This can be thought of as
     * a print statement. Anything that has semantic meaning like a process log
     * should use a different schema.
     */
    val PONumString = 1073742080
    val PODFMaskString = "64.0.1.0/32"
    val PODFString = (64, 0, 1, 0)
    val POMaskString = 32

    /*
     * GilesQueryError (2.0.8.9/32): Giles Query Error
     * A dictionary containing an error returned by a query. Has 3 keys: -
     * Query: the string query that was sent - Nonce: the nonce in the query
     * request - Error: string of the returned error
     */
    val PONumGilesQueryError = 33556489
    val PODFMaskGilesQueryError = "2.0.8.9/32"
    val PODFGilesQueryError = (2, 0, 8, 9)
    val POMaskGilesQueryError = 32

}