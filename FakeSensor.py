from bluetooth import *

# FAKE SENSOR
# THE PAIR MUST BE PAIRED A PRIORI
# ALL FAKE DATA JUST TO TEST
# TO BE REPLACED WITH ANOTHER APP IF THERE IS TIME

server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "00001101-0000-1000-8000-00805f9b34fb"

advertise_service( server_sock, "SampleServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
#                   protocols = [ OBEX_UUID ] 
                    )
                   
print("Waiting for connection on RFCOMM channel %d" % port)

client_sock, client_info = server_sock.accept()
print("Accepted connection from ", client_info)

try:
    while True:
        data = client_sock.recv(1024)
        if len(data) == 0: break
        print("received [%s]" % data)
	client_sock.send("{\"MEASURES\": [ { \"TYPE\": \"TEMPERATURE\", \"LATITUDE\": 0, \"LONGITUDE\": 0,\"MEASUREMENT_X\": 23, \"TIME\": 1478826986}]}\n")
except IOError:
    pass

print("disconnected")

client_sock.close()
server_sock.close()
print("all done")
