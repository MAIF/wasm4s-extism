//package org.extism.sdk;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import org.extism.sdk.wasm.WasmSource;
//
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//class CorazaPluginConfig {
//    public static int rootContextIds = 100;
//    public static JsonElement testRules = JsonParser.parseString("{\n" +
//            "  \"directives_map\": {\n" +
//            "    \"default\": []\n" +
//            "  },\n" +
//            "  \"rules\": [\n" +
//            "    \"SecDebugLogLevel 9\",\n" +
//            "    \"SecRuleEngine On\",\n" +
//            "    \"SecRule REQUEST_URI \\\"@streq /admin\\\" \\\"id:101,phase:1,t:lowercase,deny\\\"\"\n" +
//            "  ],\n" +
//            "  \"default_directive\": \"default\"\n" +
//            "}");
//    public static JsonElement corazaDefaultRules = JsonParser.parseString("{\n" +
//            "  \"directives_map\": {\n" +
//            "    \"default\": [\n" +
//            "      \"Include @recommended-conf\",\n" +
//            "      \"Include @crs-setup-conf\",\n" +
//            "      \"Include @owasp_crs/*.conf\",\n" +
//            "      \"SecRuleEngine On\"\n" +
//            "    ]\n" +
//            "  },\n" +
//            "  \"default_directives\": \"default\",\n" +
//            "  \"metric_labels\": {},\n" +
//            "  \"per_authority_directives\": {}\n" +
//            "}");
//}
//
//class CorazaPluginKeys {
//    public static String CorazaContextIdKey = "otoroshi.next.plugins.CorazaContextId";
//    public static String CorazaWasmVmKey    = "otoroshi.next.plugins.CorazaWasmVm";
//}
//
//record WasmVmKillOptions(
//        boolean immortal,
//        int maxCalls,
//        double maxMemoryUsage,
//        double maxAvgCallDuration,
//        double maxUnusedDuration
//) {}
//
//record WasmConfig(
//        WasmSource source,
//        int memoryPages,
//        Optional<String> functionName,
//        Map<String, String> config,
//        List<String> allowedHosts,
//        Map<String, String> allowedPaths,
//
//        Boolean wasi,
//        Boolean opa,
//        int instances,
//        WasmVmKillOptions killOptions,
//        WasmAuthorizations authorizations
//) {}
//
//record WasmDataRights(boolean read, boolean write) {}
//
//record WasmAuthorizations(
//        boolean httpAccess,
//        WasmDataRights globalDataStoreAccess,
//        WasmDataRights pluginDataStoreAccess,
//        WasmDataRights globalMapAccess,
//        WasmDataRights pluginMapAccess,
//        boolean proxyStateAccess,
//        boolean configurationAccess,
//        int proxyHttpCallTimeout
//) {}
//
//class Env {}
//
//class ProxyWasmState {
//
//    public int rootContextId;
//    public int contextId;
//    public Env env;
//
//    public ProxyWasmState(
//            int rootContextId,
//            int contextId,
//            Env env
//    ) {
//        this.rootContextId = rootContextId;
//        this.contextId = contextId;
//        this.env = env;
//    }
//
//    private int u32Len = 4;
//
//    void traceVmHost(String message) {
//        System.out.println(message);
//    }
//
//  T unimplementedFunction<T>(String name) throw Exception {
//      System.out.println("unimplemented state function: " + name);
//      throw new Exception("proxy state method is not implemented");
//  }
//
//  Result proxyLog(plugin: ExtismCurrentPlugin, logLevel: Int, messageData: Int, messageSize: Int) {
//    getMemory(plugin, messageData, messageSize)
//      .fold(
//        Error.toResult,
//        r => {
//          val message = r._2.utf8String
//          logLevel match {
//            case 0 =>
//              logger.trace(message)
//              logCallback.foreach(_.apply(org.slf4j.event.Level.TRACE, message))
//            case 1 =>
//              logger.debug(message)
//              logCallback.foreach(_.apply(org.slf4j.event.Level.DEBUG, message))
//            case 2 =>
//              logger.info(message)
//              logCallback.foreach(_.apply(org.slf4j.event.Level.INFO, message))
//            case 3 =>
//              logger.warn(message)
//              logCallback.foreach(_.apply(org.slf4j.event.Level.WARN, message))
//            case _ =>
//              logger.error(message)
//              logCallback.foreach(_.apply(org.slf4j.event.Level.ERROR, message))
//          }
//          ResultOk
//        }
//      )
//  }
//
//  Result proxyResumeStream(plugin: ExtismCurrentPlugin, StreamType streamType) {
//    traceVmHost("proxy_resume_stream");
//    return null
//  }
//
//  Result proxyCloseStream(ExtismCurrentPlugin plugin, StreamType streamType) {
//    traceVmHost("proxy_close_stream");
//    return null;
//  }
//
//  def proxySendHttpResponse(
//      plugin: ExtismCurrentPlugin,
//      responseCode: Int,
//      responseCodeDetailsData: Int,
//      responseCodeDetailsSize: Int,
//      responseBodyData: Int,
//      responseBodySize: Int,
//      additionalHeadersMapData: Int,
//      additionalHeadersSize: Int,
//      grpcStatus: Int,
//      vmData: VmData
//  ): Result = {
//    traceVmHost(s"proxy_send_http_response: ${responseCode} - ${grpcStatus}")
//    for {
//      codeDetails <- getMemory(plugin, responseCodeDetailsData, responseCodeDetailsSize)
//      body        <- getMemory(plugin, responseBodyData, responseBodySize)
//      addHeaders  <- getMemory(plugin, additionalHeadersMapData, additionalHeadersSize)
//    } yield {
//      //WasmContextSlot.getCurrentContext().map(_.asInstanceOf[VmData]).foreach { vmdata =>
//      // Json.obj(
//      //   "http_status" -> responseCode,
//      //   "grpc_code" -> grpcStatus,
//      //   "details" -> codeDetails._2.utf8String,
//      //   "body" -> body._2.utf8String,
//      //   "headers" -> addHeaders._2.utf8String,
//      // ).prettify.debugPrintln
//      vmData.respRef.set(
//        play.api.mvc.Results
//          .Status(responseCode)(body._2)
//          .withHeaders()    // TODO: read it
//          .as("text/plain") // TODO: change it
//      )
//      //}
//    }
//    ResultOk
//  }
//
//  def proxyResumeHttpStream(plugin: ExtismCurrentPlugin, streamType: StreamType): Result = {
//    traceVmHost("proxy_resume_http_stream")
//    null
//  }
//
//  def proxyCloseHttpStream(plugin: ExtismCurrentPlugin, streamType: StreamType): Result = {
//    traceVmHost("proxy_close_http_stream")
//    null
//  }
//
//  def getBuffer(plugin: ExtismCurrentPlugin, data: VmData, bufferType: BufferType): IoBuffer = {
//    bufferType match {
//      case BufferTypeHttpRequestBody      =>
//        getHttpRequestBody(plugin, data)
//      case BufferTypeHttpResponseBody     =>
//        getHttpResponseBody(plugin, data)
//      case BufferTypeDownstreamData       =>
//        getDownStreamData(plugin, data)
//      case BufferTypeUpstreamData         =>
//        getUpstreamData(plugin, data)
//      //            case BufferTypeHttpCalloutResponseBody:
//      //                GetHttpCalloutResponseBody(plugin, data)
//      case BufferTypePluginConfiguration  =>
//        getPluginConfig(plugin, data)
//      case BufferTypeVmConfiguration      =>
//        getVmConfig(plugin, data)
//      case BufferTypeHttpCallResponseBody =>
//        getHttpCalloutResponseBody(plugin, data)
//      case _                              =>
//        getCustomBuffer(bufferType)
//    }
//  }
//
//  def proxyGetBuffer(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      bufferType: Int,
//      offset: Int,
//      mSize: Int,
//      returnBufferData: Int,
//      returnBufferSize: Int
//  ): Result = {
//    traceVmHost("proxy_get_buffer")
//
//    getMemory(plugin)
//      .fold(
//        _ => ResultBadArgument,
//        memory => {
//          if (bufferType > BufferType.last) {
//            return ResultBadArgument
//          }
//          val bufferTypePluginConfiguration = getBuffer(plugin, data, BufferType.valueToType(bufferType))
//
//          var maxSize = mSize
//          if (offset > offset + maxSize) {
//            return ResultBadArgument
//          }
//          if (offset + maxSize > bufferTypePluginConfiguration.length) {
//            maxSize = bufferTypePluginConfiguration.length - offset
//          }
//
//          bufferTypePluginConfiguration.drain(offset, offset + maxSize)
//
//          //System.out.println(String.format("%s, %d,%d, %d, %d,", BufferType.valueToType(bufferType), offset, maxSize, returnBufferData, returnBufferSize))
//          return copyIntoInstance(plugin, memory, bufferTypePluginConfiguration, returnBufferData, returnBufferSize)
//        }
//      )
//  }
//
//  def proxySetBuffer(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      bufferType: Int,
//      offset: Int,
//      size: Int,
//      bufferData: Int,
//      bufferSize: Int
//  ): Result = plugin.synchronized {
//    traceVmHost("proxy_set_buffer")
//    val buf = getBuffer(plugin, data, BufferType.valueToType(bufferType))
//    if (buf == null) {
//      return ResultBadArgument
//    }
//
//    val memory: Pointer = plugin.getLinearMemory("memory")
//
//    val content = new Array[Byte](bufferSize)
//    memory.read(bufferData, content, 0, bufferSize)
//
//    if (offset == 0) {
//      if (size == 0 || size >= buf.length) {
//        buf.drain(buf.length, -1)
//        buf.write(ByteString(content))
//      } else {
//        return ResultBadArgument
//      }
//    } else if (offset >= buf.length) {
//      buf.write(ByteString(content))
//    } else {
//      return ResultBadArgument
//    }
//
//    ResultOk
//  }
//
//  def getMap(plugin: ExtismCurrentPlugin, vmData: VmData, mapType: MapType): Map[String, ByteString] = {
//    mapType match {
//      case MapTypeHttpRequestHeaders       => getHttpRequestHeader(plugin, vmData)
//      case MapTypeHttpRequestTrailers      => getHttpRequestTrailer(plugin, vmData)
//      case MapTypeHttpRequestMetadata      => getHttpRequestMetadata(plugin, vmData)
//      case MapTypeHttpResponseHeaders      => getHttpResponseHeader(plugin, vmData)
//      case MapTypeHttpResponseTrailers     => getHttpResponseTrailer(plugin, vmData)
//      case MapTypeHttpResponseMetadata     => getHttpResponseMetadata(plugin, vmData)
//      case MapTypeHttpCallResponseHeaders  => getHttpCallResponseHeaders(plugin, vmData)
//      case MapTypeHttpCallResponseTrailers => getHttpCallResponseTrailer(plugin, vmData)
//      case MapTypeHttpCallResponseMetadata => getHttpCallResponseMetadata(plugin, vmData)
//      case _                               => getCustomMap(plugin, vmData, mapType)
//    }
//  }
//
//  def copyMapIntoInstance(
//      m: Map[String, String],
//      plugin: ExtismCurrentPlugin,
//      returnMapData: Int,
//      returnMapSize: Int
//  ): Unit = unimplementedFunction("copyMapIntoInstance")
//
//  def proxyGetHeaderMapPairs(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      mapType: Int,
//      returnDataPtr: Int,
//      returnDataSize: Int
//  ): Int = {
//    traceVmHost("proxy_get_map")
//    val header = getMap(plugin, data, MapType.valueToType(mapType))
//
//    if (header == null) {
//      return ResultNotFound.value
//    }
//
//    var totalBytesLen = u32Len
//
//    header.foreach(entry => {
//      val key   = entry._1
//      val value = entry._2
//
//      totalBytesLen += u32Len + u32Len                     // keyLen + valueLen
//      totalBytesLen += key.length() + 1 + value.length + 1 // key + \0 + value + \0
//    })
//
//    // TODO - try to call proxy_on_memory_allocate
//    val addr = plugin.alloc(totalBytesLen)
//
//    // TODO - manage error
////        if err != nil {
////            return int32(v2.ResultInvalidMemoryAccess)
////        }
//
//    plugin.synchronized {
//      val memory: Pointer = plugin.getLinearMemory("memory")
//      memory.setInt(addr, header.size)
//      //        if err != nil {
//      //            return int32(v2.ResultInvalidMemoryAccess)
//      //        }
//
//      var lenPtr  = addr + u32Len
//      var dataPtr = lenPtr + (u32Len + u32Len) * header.size
//
//      header.foreach(entry => {
//        val k = entry._1
//        val v = entry._2
//
//        memory.setInt(lenPtr, k.length())
//        lenPtr += u32Len
//        memory.setInt(lenPtr, v.length)
//        lenPtr += u32Len
//
//        memory.write(dataPtr, k.getBytes(StandardCharsets.UTF_8), 0, k.length())
//        dataPtr += k.length()
//        memory.setByte(dataPtr, 0)
//        dataPtr += 1
//
//        memory.write(dataPtr, v.toArray, 0, v.length)
//        dataPtr += v.length
//        memory.setByte(dataPtr, 0)
//        dataPtr += 1
//      })
//
//      memory.setInt(returnDataPtr, addr)
//      //        if err != nil {
//      //            return int32(v2.ResultInvalidMemoryAccess)
//      //        }
//
//      memory.setInt(returnDataSize, totalBytesLen)
//      //        if err != nil {
//      //            return int32(v2.ResultInvalidMemoryAccess)
//      //        }
//
//    }
//    ResultOk.value
//  }
//
//  def proxyGetHeaderMapValue(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      mapType: Int,
//      keyData: Int,
//      keySize: Int,
//      valueData: Int,
//      valueSize: Int
//  ): Result = {
//    traceVmHost("proxy_get_header_map_value")
//    val m = getMap(plugin, data, MapType.valueToType(mapType))
//
//    if (m == null || keySize == 0) {
//      return ResultNotFound
//    }
//
//    getMemory(plugin, keyData, keySize)
//      .fold(
//        Error.toResult,
//        mem => {
//          val key = mem._2
//
//          if (key.isEmpty) {
//            ResultBadArgument
//          } else {
//            val value = m.get(key.utf8String)
//            value
//              .map(v => copyIntoInstance(plugin, mem._1, new IoBuffer(v), valueData, valueSize))
//              .getOrElse(ResultNotFound)
//          }
//        }
//      )
//  }
//
//  def proxyReplaceHeaderMapValue(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      mapType: Int,
//      keyData: Int,
//      keySize: Int,
//      valueData: Int,
//      valueSize: Int
//  ): Result = {
//    traceVmHost("proxy_set_map_value")
//    val m = getMap(plugin, data, MapType.valueToType(mapType))
//
//    if (m == null || keySize == 0) {
//      return ResultNotFound
//    }
//
//    val memKey   = getMemory(plugin, keyData, keySize)
//    val memValue = getMemory(plugin, valueData, valueSize)
//
//    memKey
//      .fold(
//        Error.toResult,
//        key => {
//          memValue.fold(
//            Error.toResult,
//            value => {
//              if (key._2.isEmpty) {
//                return ResultBadArgument
//              }
//
//              // TODO - not working
//              m ++ Map(key._2.utf8String -> value._2)
//
//              ResultOk
//            }
//          )
//        }
//      )
//  }
//
//  def proxyOpenSharedKvstore(
//      plugin: ExtismCurrentPlugin,
//      kvstoreNameData: Int,
//      kvstoreNameSiz: Int,
//      createIfNotExist: Int,
//      kvstoreID: Int
//  ): Result = unimplementedFunction("proxyOpenSharedKvstore")
//
//  def proxyGetSharedKvstoreKeyValues(
//      plugin: ExtismCurrentPlugin,
//      kvstoreID: Int,
//      keyData: Int,
//      keySize: Int,
//      returnValuesData: Int,
//      returnValuesSize: Int,
//      returnCas: Int
//  ): Result = unimplementedFunction("proxyGetSharedKvstoreKeyValues")
//
//  def proxySetSharedKvstoreKeyValues(
//      plugin: ExtismCurrentPlugin,
//      kvstoreID: Int,
//      keyData: Int,
//      keySize: Int,
//      valuesData: Int,
//      valuesSize: Int,
//      cas: Int
//  ): Result = unimplementedFunction("proxySetSharedKvstoreKeyValues")
//
//  def proxyAddSharedKvstoreKeyValues(
//      plugin: ExtismCurrentPlugin,
//      kvstoreID: Int,
//      keyData: Int,
//      keySize: Int,
//      valuesData: Int,
//      valuesSize: Int,
//      cas: Int
//  ): Result = unimplementedFunction("proxyAddSharedKvstoreKeyValues")
//
//  def proxyRemoveSharedKvstoreKey(
//      plugin: ExtismCurrentPlugin,
//      kvstoreID: Int,
//      keyData: Int,
//      keySize: Int,
//      cas: Int
//  ): Result = unimplementedFunction("proxyRemoveSharedKvstoreKey")
//
//  def proxyDeleteSharedKvstore(plugin: ExtismCurrentPlugin, kvstoreID: Int): Result = unimplementedFunction(
//    "proxyDeleteSharedKvstore"
//  )
//
//  def proxyOpenSharedQueue(
//      plugin: ExtismCurrentPlugin,
//      queueNameData: Int,
//      queueNameSize: Int,
//      createIfNotExist: Int,
//      returnQueueID: Int
//  ): Result = unimplementedFunction("proxyOpenSharedQueue")
//
//  def proxyDequeueSharedQueueItem(
//      plugin: ExtismCurrentPlugin,
//      queueID: Int,
//      returnPayloadData: Int,
//      returnPayloadSize: Int
//  ): Result = unimplementedFunction("proxyDequeueSharedQueueItem")
//
//  def proxyEnqueueSharedQueueItem(
//      plugin: ExtismCurrentPlugin,
//      queueID: Int,
//      payloadData: Int,
//      payloadSize: Int
//  ): Result = unimplementedFunction("proxyEnqueueSharedQueueItem")
//
//  def proxyDeleteSharedQueue(plugin: ExtismCurrentPlugin, queueID: Int): Result = unimplementedFunction(
//    "proxyDeleteSharedQueue"
//  )
//
//  def proxyCreateTimer(plugin: ExtismCurrentPlugin, period: Int, oneTime: Int, returnTimerID: Int): Result =
//    unimplementedFunction("proxyCreateTimer")
//
//  def proxyDeleteTimer(plugin: ExtismCurrentPlugin, timerID: Int): Result = unimplementedFunction(
//    "proxyDeleteTimer"
//  )
//
//  def proxyCreateMetric(
//      plugin: ExtismCurrentPlugin,
//      metricType: MetricType,
//      metricNameData: Int,
//      metricNameSize: Int,
//      returnMetricID: Int
//  ): MetricType = unimplementedFunction("proxyCreateMetric")
//
//  def proxyGetMetricValue(plugin: ExtismCurrentPlugin, metricID: Int, returnValue: Int): Result = {
//    // TODO - get metricID
//    val value = 10
//
//    getMemory(plugin)
//      .fold(
//        _ => ResultBadArgument,
//        mem => {
//          mem.setInt(returnValue, value)
//          ResultOk
//        }
//      )
//  }
//
//  def proxySetMetricValue(plugin: ExtismCurrentPlugin, metricID: Int, value: Int): Result =
//    unimplementedFunction("proxySetMetricValue")
//
//  def proxyIncrementMetricValue(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      metricID: Int,
//      offset: Long
//  ): Result = {
//    traceVmHost("proxy_increment_metric")
//    ResultOk
//  }
//
//  def proxyDeleteMetric(plugin: ExtismCurrentPlugin, metricID: Int): Result = unimplementedFunction(
//    "proxyDeleteMetric"
//  )
//
//  def proxyDefineMetric(
//      plugin: ExtismCurrentPlugin,
//      metricType: Int,
//      namePtr: Int,
//      nameSize: Int,
//      returnMetricId: Int
//  ): Result = {
//    traceVmHost("proxy_define_metric")
//    if (metricType > MetricType.last) {
//      ResultBadArgument
//    } else {
//
//      getMemory(plugin, namePtr, nameSize)
//        .fold(
//          _ => ResultBadArgument,
//          mem => {
//            // mid = ih.DefineMetric(v1.MetricType(metricType), mem._2.utf8String)
//
//            val mid = 1
//            mem._1.setInt(returnMetricId, mid)
//            ResultOk
//          }
//        )
//    }
//  }
//
//  def proxyDispatchHttpCall(
//      plugin: ExtismCurrentPlugin,
//      upstreamNameData: Int,
//      upstreamNameSize: Int,
//      headersMapData: Int,
//      headersMapSize: Int,
//      bodyData: Int,
//      bodySize: Int,
//      trailersMapData: Int,
//      trailersMapSize: Int,
//      timeoutMilliseconds: Int,
//      returnCalloutID: Int
//  ): Result = unimplementedFunction("proxyDispatchHttpCall")
//
//  def proxyDispatchGrpcCall(
//      plugin: ExtismCurrentPlugin,
//      upstreamNameData: Int,
//      upstreamNameSize: Int,
//      serviceNameData: Int,
//      serviceNameSize: Int,
//      serviceMethodData: Int,
//      serviceMethodSize: Int,
//      initialMetadataMapData: Int,
//      initialMetadataMapSize: Int,
//      grpcMessageData: Int,
//      grpcMessageSize: Int,
//      timeoutMilliseconds: Int,
//      returnCalloutID: Int
//  ): Result = unimplementedFunction("proxyDispatchGrpcCall")
//
//  def proxyOpenGrpcStream(
//      plugin: ExtismCurrentPlugin,
//      upstreamNameData: Int,
//      upstreamNameSize: Int,
//      serviceNameData: Int,
//      serviceNameSize: Int,
//      serviceMethodData: Int,
//      serviceMethodSize: Int,
//      initialMetadataMapData: Int,
//      initialMetadataMapSize: Int,
//      returnCalloutID: Int
//  ): Result = unimplementedFunction("proxyOpenGrpcStream")
//
//  def proxySendGrpcStreamMessage(
//      plugin: ExtismCurrentPlugin,
//      calloutID: Int,
//      grpcMessageData: Int,
//      grpcMessageSize: Int
//  ): Result = unimplementedFunction("proxySendGrpcStreamMessage")
//
//  def proxyCancelGrpcCall(plugin: ExtismCurrentPlugin, calloutID: Int): Result = unimplementedFunction(
//    "proxyCancelGrpcCall"
//  )
//
//  def proxyCloseGrpcCall(plugin: ExtismCurrentPlugin, calloutID: Int): Result = unimplementedFunction(
//    "proxyCloseGrpcCall"
//  )
//
//  def proxyCallCustomFunction(
//      plugin: ExtismCurrentPlugin,
//      customFunctionID: Int,
//      parametersData: Int,
//      parametersSize: Int,
//      returnResultsData: Int,
//      returnResultsSize: Int
//  ): Result = unimplementedFunction("proxyCallCustomFunction")
//
//  def copyIntoInstance(
//      plugin: ExtismCurrentPlugin,
//      memory: Pointer,
//      value: IoBuffer,
//      retPtr: Int,
//      retSize: Int
//  ): Result = {
//    val addr = plugin.alloc(value.length)
//
//    memory.write(addr, value.buf.toArray, 0, value.length)
//
//    memory.setInt(retPtr, addr)
//    memory.setInt(retSize, value.length)
//
//    ResultOk
//  }
//
//  def proxyGetProperty(
//      plugin: ExtismCurrentPlugin,
//      data: VmData,
//      pathPtr: Int,
//      pathSize: Int,
//      returnValueData: Int,
//      returnValueSize: Int
//  ): Result = {
//    traceVmHost("proxy_get_property")
//    val mem = getMemory(plugin, pathPtr, pathSize)
//
//    mem.fold(
//      _ => ResultBadArgument,
//      m => {
//        if (m._2.isEmpty) {
//          ResultBadArgument
//        } else {
//          val path = m._2.utf8String
//            .replace(Character.toString(0), ".")
//
//          val value: Array[Byte] = data.properties.getOrElse(path, ByteString.empty.toArray)
//
//          if (value == null) {
//            return ResultNotFound
//          }
//
//          copyIntoInstance(plugin, m._1, new IoBuffer(ByteString(value)), returnValueData, returnValueSize)
//        }
//      }
//    )
//  }
//
//  def proxyRegisterSharedQueue(nameData: ByteString, nameSize: Int, returnID: Int): Status =
//    unimplementedFunction("proxyRegisterSharedQueue")
//
//  def proxyResolveSharedQueue(
//      vmIDData: ByteString,
//      vmIDSize: Int,
//      nameData: ByteString,
//      nameSize: Int,
//      returnID: Int
//  ): Status = unimplementedFunction("proxyResolveSharedQueue")
//
//  def proxyEnqueueSharedQueue(queueID: Int, valueData: ByteString, valueSize: Int): Status =
//    unimplementedFunction("proxyEnqueueSharedQueue")
//
//  def proxyDequeueSharedQueue(queueID: Int, returnValueData: ByteString, returnValueSize: Int): Status =
//    unimplementedFunction("proxyDequeueSharedQueue")
//
//  def proxyDone(): Status = {
//    StatusOK
//  }
//
//  def proxySetTickPeriodMilliseconds(data: VmData, period: Int): Status = {
//    // TODO - manage tick period
//    // data.setTickPeriod(period)
//    StatusOK
//  }
//
//  def proxySetEffectiveContext(plugin: ExtismCurrentPlugin, contextID: Int): Status = {
//    // TODO - manage context id changes
//    // this.contextId = contextID
//    StatusOK
//  }
//
//  def getPluginConfig(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = {
//    new IoBuffer(data.configuration)
//  }
//
//  def getHttpRequestBody(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = {
//    data.bodyIn match {
//      case None       => new IoBuffer(ByteString.empty)
//      case Some(body) => new IoBuffer(body)
//    }
//  }
//
//  def getHttpResponseBody(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = {
//    data.bodyOut match {
//      case None       => new IoBuffer(ByteString.empty)
//      case Some(body) => new IoBuffer(body)
//    }
//  }
//
//  def getDownStreamData(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = unimplementedFunction(
//    "getDownStreamData"
//  )
//
//  def getUpstreamData(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = unimplementedFunction(
//    "getUpstreamData"
//  )
//
//  def getHttpCalloutResponseBody(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = unimplementedFunction(
//    "getHttpCalloutResponseBody"
//  )
//
//  def getVmConfig(plugin: ExtismCurrentPlugin, data: VmData): IoBuffer = unimplementedFunction("getVmConfig")
//
//  def getCustomBuffer(bufferType: BufferType): IoBuffer = unimplementedFunction("getCustomBuffer")
//
//  def getHttpRequestHeader(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] = {
//    data.properties
//      .filter(entry => entry._1.startsWith("request.headers.") || entry._1.startsWith(":"))
//      .map(t => (t._1.replace("request.headers.", ""), ByteString(t._2)))
//  }
//
//  def getHttpRequestTrailer(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] = {
//    Map.empty
//  }
//
//  def getHttpRequestMetadata(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] = {
//    Map.empty
//  }
//
//  def getHttpResponseHeader(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] = {
//    data.properties
//      .filter(entry => entry._1.startsWith("response.headers.") || entry._1.startsWith(":"))
//      .map(t => (t._1.replace("response.headers.", ""), ByteString(t._2)))
//  }
//
//  def getHttpResponseTrailer(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] =
//    unimplementedFunction("getHttpResponseTrailer")
//
//  def getHttpResponseMetadata(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] =
//    unimplementedFunction("getHttpResponseMetadata")
//
//  def getHttpCallResponseHeaders(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] =
//    unimplementedFunction("getHttpCallResponseHeaders")
//
//  def getHttpCallResponseTrailer(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] =
//    unimplementedFunction("getHttpCallResponseTrailer")
//
//  def getHttpCallResponseMetadata(plugin: ExtismCurrentPlugin, data: VmData): Map[String, ByteString] =
//    unimplementedFunction("getHttpCallResponseMetadata")
//
//  def getCustomMap(plugin: ExtismCurrentPlugin, data: VmData, mapType: MapType): Map[String, ByteString] =
//    unimplementedFunction("getCustomMap")
//
//  def getMemory(plugin: ExtismCurrentPlugin, addr: Int, size: Int): Either[Error, (Pointer, ByteString)] =
//    plugin.synchronized {
//      val memory: Pointer = plugin.getLinearMemory("memory")
//      if (memory == null) {
//        return Error.ErrorExportsNotFound.left
//      }
//
//      // TODO - get memory size from RUST
////        long memoryLength = 1024 * 64 * 50// plugin.memoryLength(0)
////        if (addr > memoryLength || (addr+size) > memoryLength) {
////            return Either.left(Error.ErrAddrOverflow)
////        }
//
//      (memory -> ByteString(memory.share(addr).getByteArray(0, size))).right[Error]
//    }
//
//  def getMemory(plugin: ExtismCurrentPlugin): Either[Error, Pointer] = plugin.synchronized {
//
//    val memory: Pointer = plugin.getLinearMemory("memory")
//    if (memory == null) {
//      return Error.ErrorExportsNotFound.left
//    }
//
//    memory.right
//  }
//}
//
//class CorazaPlugin {
//
//    private WasmConfig wasm;
//    private CorazaWafConfig config;
//    private String key;
//    private Env env;
//
//    public CorazaPlugin(WasmConfig wasm, CorazaWafConfig config, String key, Env env) {
//        this.wasm = wasm;
//        this.config = config;
//        this.key = key;
//        this.env = env;
//    }
//
//    private int timeout                 = 10;
//    private boolean started             = false;
////    private var logger                  = Logger("otoroshi-plugin-coraza")
//    private int vmConfigurationSize     = 0;
//    private JsonObject rules            = config.config();
//    private byte[] pluginConfigurationSize = new Gson().toJson(rules).getBytes(StandardCharsets.UTF_8);
//    private int contextId               = 0;
//    private ProxyWasmState state        = new ProxyWasmState(1, contextId, env);
//
//
//    def createFunctions(ref: WasmVmData): Seq[HostFunction[EnvUserData]] = {
//        ProxyWasmFunctions.build(state, ref)
//    }
//
//    def callPluginWithoutResults(
//            function: String,
//            params: Parameters,
//            data: VmData,
//            attrs: TypedMap,
//            shouldBeCallOnce: Boolean = false
//    ): Future[Either[JsValue, ResultsWrapper]] = {
//        attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey) match {
//            case None     =>
//                /* TODO - REPLACE WITH logger.error( */ println("no vm found in attrs")
//                Left(Json.obj("error" -> "no vm found in attrs")).vfuture
//            case Some(vm) => {
//                WasmUtils.traceHostVm(function + s" - vm: ${vm.index}")
//                vm.call(WasmFunctionParameters.NoResult(function, params), Some(data))
//                        .map { opt =>
//                    opt.map { res =>
//                        res._2.free()
//                        res._2
//                    }
//                }
//          .andThen { case _ =>
//                    vm.release()
//                }
//            }
//        }
//    }
//
//    def callPluginWithResults(
//            function: String,
//            params: Parameters,
//            results: Int,
//            data: VmData,
//            attrs: TypedMap,
//            shouldBeCallOnce: Boolean = false
//    ): Future[ResultsWrapper] = {
//        attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey) match {
//            case None     =>
//                /* TODO - REPLACE WITH logger.error( */ println("no vm found in attrs")
//                Future.failed(new RuntimeException("no vm found in attrs"))
//            case Some(vm) => {
//                WasmUtils.traceHostVm(function + s" - vm: ${vm.index}")
//                vm.call(WasmFunctionParameters.BothParamsResults(function, params, results), Some(data))
//                        .flatMap {
//                    case Left(err)           =>
//                        /* TODO - REPLACE WITH logger.error( */ println(s"error while calling plugin: ${err}")
//                        Future.failed(new RuntimeException(s"callPluginWithResults: ${err.stringify}"))
//                    case Right((_, results)) => results.vfuture
//                }
//          .andThen { case _ =>
//                    vm.release()
//                }
//            }
//        }
//    }
//
//    def proxyOnContexCreate(contextId: Int, rootContextId: Int, attrs: TypedMap, rootData: VmData): Future[Unit] = {
//        val prs = new Parameters(2)
//                .pushInts(contextId, rootContextId)
//        callPluginWithoutResults("proxy_on_context_create", prs, rootData, attrs).map(_ => ())
//        // TODO - just try to reset context for each request without call proxyOnConfigure
//    }
//
//    def proxyOnVmStart(attrs: TypedMap, rootData: VmData): Future[Boolean] = {
//        val prs = new Parameters(2)
//                .pushInts(0, vmConfigurationSize)
//        callPluginWithResults("proxy_on_vm_start", prs, 1, rootData, attrs, shouldBeCallOnce = true).map {
//            proxyOnVmStartAction =>
//            val res = proxyOnVmStartAction.results.getValues()(0).v.i32 != 0
//            proxyOnVmStartAction.free()
//            res
//        }
//    }
//
//    def proxyOnConfigure(rootContextId: Int, attrs: TypedMap, rootData: VmData): Future[Boolean] = {
//        val prs = new Parameters(2)
//                .pushInts(rootContextId, pluginConfigurationSize)
//        println(rootContextId, pluginConfigurationSize)
//        callPluginWithResults("proxy_on_configure", prs, 1, rootData, attrs, shouldBeCallOnce = true).map {
//            proxyOnConfigureAction =>
//            println("Value of proxyOnConfigureAction", proxyOnConfigureAction)
//            println(proxyOnConfigureAction.results.getValues())
//            val res = proxyOnConfigureAction.results.getValues()(0).v.i32 != 0
//            proxyOnConfigureAction.free()
//            res
//        }
//    }
//
//    def proxyOnDone(rootContextId: Int, attrs: TypedMap): Future[Boolean] = {
//        val prs      = new Parameters(1).pushInt(rootContextId)
//        val rootData = VmData.empty()
//        callPluginWithResults("proxy_on_done", prs, 1, rootData, attrs).map { proxyOnConfigureAction =>
//            val res = proxyOnConfigureAction.results.getValues()(0).v.i32 != 0
//            proxyOnConfigureAction.free()
//            res
//        }
//    }
//
//    def proxyOnDelete(rootContextId: Int, attrs: TypedMap): Future[Unit] = {
//        val prs      = new Parameters(1).pushInt(rootContextId)
//        val rootData = VmData.empty()
//        callPluginWithoutResults("proxy_on_delete", prs, rootData, attrs).map(_ => ())
//    }
//
//    def proxyStart(attrs: TypedMap, rootData: VmData): Future[ResultsWrapper] = {
//        callPluginWithoutResults("_start", new Parameters(0), rootData, attrs, shouldBeCallOnce = true).map {
//            res =>
//            res.right.get
//        }
//    }
//
//    def proxyCheckABIVersion(attrs: TypedMap, rootData: VmData): Future[Unit] = {
//        callPluginWithoutResults(
//                "proxy_abi_version_0_2_0",
//                new Parameters(0),
//                rootData,
//                attrs,
//                shouldBeCallOnce = true
//        ).map(_ => ())
//    }
//
//    def reportError(result: Result, vm: WasmVm, from: String): Unit = {
//        /* TODO - REPLACE WITH logger.error( */ println(s"[${vm.index}] from: $from - error: ${result.value} - ${vm.calls} / ${vm.current}")
//    }
//
//    def proxyOnRequestHeaders(
//            contextId: Int,
//            request: RequestHeader,
//            attrs: TypedMap
//    ): Future[Either[play.api.mvc.Result, Unit]] = {
//        val vm          = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).get
//        val data        = VmData.empty().withRequest(request, attrs)(env)
//                val endOfStream = 1
//        val sizeHeaders = 0
//        val prs         = new Parameters(3).pushInts(contextId, sizeHeaders, endOfStream)
//        callPluginWithResults("proxy_on_request_headers", prs, 1, data, attrs).map { requestHeadersAction =>
//            val result = Result.valueToType(requestHeadersAction.results.getValues()(0).v.i32)
//            requestHeadersAction.free()
//            if (result != Result.ResultOk || data.httpResponse.isDefined) {
//                data.httpResponse match {
//                    case None           =>
//                        reportError(result, vm, "proxyOnRequestHeaders")
//                        Left(
//                                play.api.mvc.Results
//                                        .InternalServerError(Json.obj("error" -> s"no http response in context 1: ${result.value}"))
//            ) // TODO: not sure if okay
//                    case Some(response) => Left(response)
//                }
//            } else {
//                Right(())
//            }
//        }
//    }
//
//    def proxyOnRequestBody(
//            contextId: Int,
//            request: RequestHeader,
//            req: NgPluginHttpRequest,
//            body_bytes: ByteString,
//            attrs: TypedMap
//    ): Future[Either[play.api.mvc.Result, Unit]] = {
//        val vm          = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).get
//        val data        = VmData.empty().withRequest(request, attrs)(env)
//                data.bodyInRef.set(body_bytes)
//        val endOfStream = 1
//        val sizeBody    = body_bytes.size.bytes.length
//        val prs         = new Parameters(3).pushInts(contextId, sizeBody, endOfStream)
//        callPluginWithResults("proxy_on_request_body", prs, 1, data, attrs).map { requestHeadersAction =>
//            val result = Result.valueToType(requestHeadersAction.results.getValues()(0).v.i32)
//            requestHeadersAction.free()
//            if (result != Result.ResultOk || data.httpResponse.isDefined) {
//                data.httpResponse match {
//                    case None           =>
//                        reportError(result, vm, "proxyOnRequestBody")
//                        Left(
//                                play.api.mvc.Results
//                                        .InternalServerError(Json.obj("error" -> s"no http response in context 2: ${result.value}"))
//            ) // TODO: not sure if okay
//                    case Some(response) => Left(response)
//                }
//            } else {
//                Right(())
//            }
//        }
//    }
//
//    def proxyOnResponseHeaders(
//            contextId: Int,
//            response: NgPluginHttpResponse,
//            attrs: TypedMap
//    ): Future[Either[play.api.mvc.Result, Unit]] = {
//        val vm          = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).get
//        val data        = VmData.empty().withResponse(response, attrs)(env)
//                val endOfStream = 1
//        val sizeHeaders = 0
//        val prs         = new Parameters(3).pushInts(contextId, sizeHeaders, endOfStream)
//        callPluginWithResults("proxy_on_response_headers", prs, 1, data, attrs).map { requestHeadersAction =>
//            val result = Result.valueToType(requestHeadersAction.results.getValues()(0).v.i32)
//            requestHeadersAction.free()
//            if (result != Result.ResultOk || data.httpResponse.isDefined) {
//                data.httpResponse match {
//                    case None           =>
//                        reportError(result, vm, "proxyOnResponseHeaders")
//                        Left(
//                                play.api.mvc.Results
//                                        .InternalServerError(Json.obj("error" -> s"no http response in context 3: ${result.value}"))
//            ) // TODO: not sure if okay
//                    case Some(response) => Left(response)
//                }
//            } else {
//                Right(())
//            }
//        }
//    }
//
//    def proxyOnResponseBody(
//            contextId: Int,
//            response: NgPluginHttpResponse,
//            body_bytes: ByteString,
//            attrs: TypedMap
//    ): Future[Either[play.api.mvc.Result, Unit]] = {
//        val vm          = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).get
//        val data        = VmData.empty().withResponse(response, attrs)(env)
//                data.bodyInRef.set(body_bytes)
//        val endOfStream = 1
//        val sizeBody    = body_bytes.size.bytes.length
//        val prs         = new Parameters(3).pushInts(contextId, sizeBody, endOfStream)
//        callPluginWithResults("proxy_on_response_body", prs, 1, data, attrs).map { requestHeadersAction =>
//            val result = Result.valueToType(requestHeadersAction.results.getValues()(0).v.i32)
//            requestHeadersAction.free()
//            if (result != Result.ResultOk || data.httpResponse.isDefined) {
//                data.httpResponse match {
//                    case None           =>
//                        reportError(result, vm, "proxyOnResponseBody")
//                        Left(
//                                play.api.mvc.Results
//                                        .InternalServerError(Json.obj("error" -> s"no http response in context 4: ${result.value}"))
//            ) // TODO: not sure if okay
//                    case Some(response) => Left(response)
//                }
//            } else {
//                Right(())
//            }
//        }
//    }
//
//    def start(attrs: TypedMap): Future[Unit] = {
//        pool.getPooledVm(WasmVmInitOptions(false, true, createFunctions)).flatMap { vm =>
//            val data = VmData.withRules(rules)
//            attrs.put(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey -> vm)
//            vm.finitialize {
//                proxyStart(attrs, data).flatMap { _ =>
//                    proxyCheckABIVersion(attrs, data).flatMap { _ =>
//                        // according to ABI, we should create a root context id before any operations
//                        proxyOnContexCreate(state.rootContextId, 0, attrs, data).flatMap { _ =>
//                            proxyOnVmStart(attrs, data).flatMap {
//                                case true =>
//                                    proxyOnConfigure(state.rootContextId, attrs, data).map {
//                                    case true => started.set(true)
//                                    case _    => /* TODO - REPLACE WITH logger.error( */ println("failed to configure coraza")
//                                }
//                                case _    => /* TODO - REPLACE WITH logger.error( */ println("failed to start coraza vm").vfuture
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    def stop(attrs: TypedMap): Future[Unit] = {
//        ().vfuture
//    }
//    // TODO - need to save VmData in attrs to get it from the start function and reuse the same slotId
//    def runRequestPath(request: RequestHeader, attrs: TypedMap): Future[NgAccess] = {
//        val contId = contextId.incrementAndGet()
//        attrs.put(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaContextIdKey -> contId)
//        val instance = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).get
//        val data     = VmData.withRules(rules)
//        proxyOnContexCreate(contId, state.rootContextId, attrs, data).flatMap { _ =>
//            proxyOnRequestHeaders(contId, request, attrs).map {
//                case Left(errRes) =>
//                    proxyOnDone(contId, attrs)
//                    proxyOnDelete(contId, attrs)
//                    NgAccess.NgDenied(errRes)
//                case Right(_)     => NgAccess.NgAllowed
//            }
//        }
//    }
//
//    def runRequestBodyPath(
//            request: RequestHeader,
//            req: NgPluginHttpRequest,
//            body_bytes: Option[ByteString],
//            attrs: TypedMap
//    ): Future[Either[mvc.Result, Unit]] = {
//        val contId = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaContextIdKey).get
//        val f      =
//        if (body_bytes.isDefined) proxyOnRequestBody(contId, request, req, body_bytes.get, attrs) else Right(()).vfuture
//        // proxy_on_http_request_trailers
//        // proxy_on_http_request_metadata : H2 only
//        f.map {
//            case Left(errRes) =>
//                proxyOnDone(contId, attrs)
//                proxyOnDelete(contId, attrs)
//                Left(errRes)
//            case Right(_)     => Right(())
//        }
//    }
//
//    def runResponsePath(
//            response: NgPluginHttpResponse,
//            body_bytes: Option[ByteString],
//            attrs: TypedMap
//    ): Future[Either[mvc.Result, Unit]] = {
//        val contId = attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaContextIdKey).get
//        proxyOnResponseHeaders(contId, response, attrs).flatMap {
//            case Left(e)  => Left(e).vfuture
//            case Right(_) => {
//                val res =
//                if (body_bytes.isDefined) proxyOnResponseBody(contId, response, body_bytes.get, attrs) else Right(()).vfuture
//                // proxy_on_http_response_trailers
//                // proxy_on_http_response_metadata : H2 only
//                proxyOnDone(contId, attrs)
//                proxyOnDelete(contId, attrs)
//                res
//            }
//        }
//    }
//}
//
//case class NgCorazaWAFConfig(ref: String) extends NgPluginConfig {
//    def json: JsValue = NgCorazaWAFConfig.format.writes(this)
//}
//
//    object NgCorazaWAFConfig {
//        val format = new Format[NgCorazaWAFConfig] {
//        def writes(o: NgCorazaWAFConfig): JsValue             = Json.obj("ref" -> o.ref)
//        def reads(json: JsValue): JsResult[NgCorazaWAFConfig] = Try {
//        NgCorazaWAFConfig(
//        ref = json.select("ref").asString
//        )
//        } match {
//        case Success(e) => JsSuccess(e)
//        case Failure(e) => JsError(e.getMessage)
//        }
//        }
//        }
//
//class NgCorazaWAF extends NgAccessValidator with NgRequestTransformer {
//
//    def steps: Seq[NgStep]                          = Seq(NgStep.ValidateAccess, NgStep.TransformRequest, NgStep.TransformResponse)
//    def categories: Seq[NgPluginCategory]           = Seq(NgPluginCategory.AccessControl)
//    def visibility: NgPluginVisibility              = NgPluginVisibility.NgUserLand
//    def multiInstance: Boolean                      = true
//    def core: Boolean                               = true
//    def name: String                                = "Coraza WAF"
//    def description: Option[String]                 = "Coraza WAF plugin".some
//    def defaultConfigObject: Option[NgPluginConfig] = NgCorazaWAFConfig("none").some
//
//    def isAccessAsync: Boolean            = true
//    def isTransformRequestAsync: Boolean  = true
//    def isTransformResponseAsync: Boolean = true
//    def usesCallbacks: Boolean            = true
//    def transformsRequest: Boolean        = true
//    def transformsResponse: Boolean       = true
//    def transformsError: Boolean          = false
//
//    private val plugins = new UnboundedTrieMap[String, CorazaPlugin]()
//
//    private def getPlugin(ref: String, attrs: TypedMap)(implicit env: Env): CorazaPlugin = plugins.synchronized {
//        val config     = env.adminExtensions.extension[CorazaWafAdminExtension].get.states.config(ref).get
//        val configHash = config.json.stringify.sha512
//        val key        = s"ref=${ref}&hash=${configHash}"
//
//        val plugin          = if (plugins.contains(key)) {
//            plugins(key)
//        } else {
//            val url = s"http://127.0.0.1:${env.httpPort}/__otoroshi_assets/wasm/coraza-proxy-wasm-v0.1.2.wasm?$key"
//            val p   = new CorazaPlugin(
//                    WasmConfig(
//                            source = WasmSource(
//                                    kind = WasmSourceKind.Http,
//                                    path = url
//                            ),
//                            memoryPages = 10000,
//                            functionName = None,
//                            wasi = true,
//                            // lifetime = WasmVmLifetime.Forever,
//                            instances = config.poolCapacity,
//                            killOptions = WasmVmKillOptions(
//                                    maxCalls = 2000,
//                                    maxMemoryUsage = 0.9,
//                                    maxAvgCallDuration = 1.day,
//                                    maxUnusedDuration = 5.minutes
//                            )
//                    ),
//                    config,
//                    url,
//                    env
//            )
//            plugins.put(key, p)
//            p
//        }
//        val oldVersionsKeys = plugins.keySet.filter(_.startsWith(s"ref=${ref}&hash=")).filterNot(_ == key)
//        val oldVersions     = oldVersionsKeys.flatMap(plugins.get)
//        if (oldVersions.nonEmpty) {
//            oldVersions.foreach(_.stop(attrs))
//            oldVersionsKeys.foreach(plugins.remove)
//        }
//        plugin
//    }
//
//    def beforeRequest(
//            ctx: NgBeforeRequestContext
//    )(implicit env: Env, ec: ExecutionContext, mat: Materializer): Future[Unit] = {
//        val config = ctx.cachedConfig(internalName)(NgCorazaWAFConfig.format).getOrElse(NgCorazaWAFConfig("none"))
//        val plugin = getPlugin(config.ref, ctx.attrs)
//        plugin.start(ctx.attrs)
//    }
//
//    def afterRequest(
//            ctx: NgAfterRequestContext
//    )(implicit env: Env, ec: ExecutionContext, mat: Materializer): Future[Unit] = {
//        ctx.attrs.get(otoroshi.wasm.proxywasm.CorazaPluginKeys.CorazaWasmVmKey).foreach(_.release())
//        ().vfuture
//    }
//
//    def access(ctx: NgAccessContext)(implicit env: Env, ec: ExecutionContext): Future[NgAccess] = {
//        val config = ctx.cachedConfig(internalName)(NgCorazaWAFConfig.format).getOrElse(NgCorazaWAFConfig("none"))
//        val plugin = getPlugin(config.ref, ctx.attrs)
//        plugin.runRequestPath(ctx.request, ctx.attrs)
//    }
//
//    def transformRequest(
//            ctx: NgTransformerRequestContext
//    )(implicit env: Env, ec: ExecutionContext, mat: Materializer): Future[Either[mvc.Result, NgPluginHttpRequest]] = {
//        val config                             = ctx.cachedConfig(internalName)(NgCorazaWAFConfig.format).getOrElse(NgCorazaWAFConfig("none"))
//        val plugin                             = getPlugin(config.ref, ctx.attrs)
//        val hasBody                            = ctx.request.theHasBody
//        val bytesf: Future[Option[ByteString]] =
//        if (!plugin.config.inspectBody) None.vfuture
//        else if (!hasBody) None.vfuture
//        else {
//            ctx.otoroshiRequest.body.runFold(ByteString.empty)(_ ++ _).map(_.some)
//        }
//        bytesf.flatMap { bytes =>
//            val req =
//            if (plugin.config.inspectBody && hasBody) ctx.otoroshiRequest.copy(body = bytes.get.chunks(16 * 1024))
//            else ctx.otoroshiRequest
//                    plugin
//        .runRequestBodyPath(
//                    ctx.request,
//                    req,
//                    bytes,
//                    ctx.attrs
//            )
//                    .map {
//                case Left(result) => Left(result)
//                case Right(_)     => Right(req)
//            }
//        }
//    }
//
//    def transformResponse(
//            ctx: NgTransformerResponseContext
//    )(implicit env: Env, ec: ExecutionContext, mat: Materializer): Future[Either[mvc.Result, NgPluginHttpResponse]] = {
//        val config                             = ctx.cachedConfig(internalName)(NgCorazaWAFConfig.format).getOrElse(NgCorazaWAFConfig("none"))
//        val plugin                             = getPlugin(config.ref, ctx.attrs)
//        val bytesf: Future[Option[ByteString]] =
//        if (!plugin.config.inspectBody) None.vfuture
//        else ctx.otoroshiResponse.body.runFold(ByteString.empty)(_ ++ _).map(_.some)
//        bytesf.flatMap { bytes =>
//            val res =
//            if (plugin.config.inspectBody) ctx.otoroshiResponse.copy(body = bytes.get.chunks(16 * 1024))
//            else ctx.otoroshiResponse
//                    plugin
//        .runResponsePath(
//                    res,
//                    bytes,
//                    ctx.attrs
//            )
//                    .map {
//                case Left(result) => Left(result)
//                case Right(_)     => Right(res)
//            }
//        }
//    }
//}
//
//record EntityLocation(String tenant, List<String> teams) {}
//
//record CorazaWafConfig(
//        EntityLocation location,
//        String id,
//        String name,
//        String description,
//        List<String> tags,
//        Map<String, String> metadata,
//        boolean inspectBody,
//        JsonObject config,
//        int poolCapacity
//) { }
//
//class WasmVmData {}
//
////            WasmIntegrationContext ic,
////            ExecutionContext executionContext,
////            Materializer mat,
////            WasmConfiguration config
//class EnvUserData  extends HostUserData {}
//
//class ProxyWasmFunctions {
//
//  List<HostFunction<EnvUserData>> build(
//      ProxyWasmState state,
//      WasmVmData vmDataRef
//  ) {
//    List.of(
//      new HostFunction<EnvUserData>(
//              "proxy_log",
//              parameters(3),
//              parameters(1),
//              (ExtismFunction) (plugin, params, returns, data) -> state.proxyLog(plugin, params(0).v.i32, params(1).v.i32, params(2).v.i32)
//        )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_get_buffer_bytes",
//        parameters(5),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) ->
//          state.proxyGetBuffer(
//            plugin,
//            vmDataRef,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32,
//            params(4).v.i32
//          )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_set_effective_context",
//        parameters(1),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) ->  state.proxySetEffectiveContext(plugin, params(0).v.i32)
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_get_header_map_pairs",
//        parameters(3),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxyGetHeaderMapPairs(plugin, vmDataRef, params(0).v.i32, params(1).v.i32, params(2).v.i32)
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_set_buffer_bytes",
//        parameters(5),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxySetBuffer(
//            plugin,
//            vmDataRef,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32,
//            params(4).v.i32
//          )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_get_header_map_value",
//        parameters(5),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxyGetHeaderMapValue(
//            plugin,
//            vmDataRef,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32,
//            params(4).v.i32
//          )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_get_property",
//        parameters(4),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxyGetProperty(
//            plugin,
//            vmDataRef,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32
//          )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_increment_metric",
//        Seq(LibExtism.ExtismValType.I32, LibExtism.ExtismValType.I64).toArray,
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) ->  state.proxyIncrementMetricValue(plugin, vmDataRef, params(0).v.i32, params(1).v.i64)
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_define_metric",
//        parameters(4),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) ->  state.proxyDefineMetric(plugin, params(0).v.i32, params(1).v.i32, params(2).v.i32, params(3).v.i32)
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_set_tick_period_milliseconds",
//        parameters(1),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxySetTickPeriodMilliseconds(vmDataRef, params(0).v.i32)
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_replace_header_map_value",
//        parameters(5),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxyReplaceHeaderMapValue(
//            plugin,
//            vmDataRef,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32,
//            params(4).v.i32
//          )
//      ).withNamespace("env"),
//      new HostFunction<EnvUserData>(
//        "proxy_send_local_response",
//        parameters(8),
//        parameters(1),
//        (ExtismFunction) (plugin, params, returns, data) -> state.proxySendHttpResponse(
//            plugin,
//            params(0).v.i32,
//            params(1).v.i32,
//            params(2).v.i32,
//            params(3).v.i32,
//            params(4).v.i32,
//            params(5).v.i32,
//            params(6).v.i32,
//            params(7).v.i32,
//            vmDataRef
//          )
//      ).withNamespace("env")
//    )
//  }
//
//  private List<LibExtism.ExtismValType> parameters(int n) {
//      List<LibExtism.ExtismValType> out = new ArrayList<>();
//      for (int i = 0 ; i< n; i++) {
//          out.add(LibExtism.ExtismValType.I32);
//      }
//
//      return out;
//  }
//}
