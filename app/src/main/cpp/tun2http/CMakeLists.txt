cmake_minimum_required(VERSION 3.4.1)

#set(CMAKE_CXX_FLAGS "-std=c++11")

include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/tun2http)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_SOURCE_DIR}/src/main/assets/${ANDROID_ABI}")

add_library( 
            # Sets the name of the library.
            tun2http
            # Sets the library as a shared library.
            SHARED
            dhcp.c
            dns.c
            icmp.c
            ip.c
            http.c
            tun2http.c
            session.c
            tcp.c
            tls.c
            udp.c
            util.c )

find_library( 
              # Sets the name of the path variable.
              log-lib
              log )

target_link_libraries(
                      # Specifies the target library.
                      tun2http
                      ${log-lib} )