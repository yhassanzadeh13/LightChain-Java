// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: message.proto

package network.p2p.proto;

public final class MessageOuterClass {
  private MessageOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_network_p2p_proto_Message_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_network_p2p_proto_Message_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\rmessage.proto\022\021network.p2p.proto\032\033goog" +
      "le/protobuf/empty.proto\"^\n\007Message\022\020\n\010Or" +
      "iginId\030\001 \001(\014\022\017\n\007Channel\030\002 \001(\t\022\021\n\tTargetI" +
      "ds\030\003 \003(\014\022\017\n\007Payload\030\004 \001(\014\022\014\n\004Type\030\005 \001(\t2" +
      "N\n\tMessenger\022A\n\007Deliver\022\032.network.p2p.pr" +
      "oto.Message\032\026.google.protobuf.Empty\"\000(\001B" +
      "\002P\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.EmptyProto.getDescriptor(),
        });
    internal_static_network_p2p_proto_Message_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_network_p2p_proto_Message_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_network_p2p_proto_Message_descriptor,
        new java.lang.String[] { "OriginId", "Channel", "TargetIds", "Payload", "Type", });
    com.google.protobuf.EmptyProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
