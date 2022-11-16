FROM bellsoft/liberica-openjdk-alpine-musl:11 AS server-build
# Copy server java source files
COPY ./server/*.java /usr/src/server/
# Copy logger properties for the server
COPY ./server/*.properties /usr/config/
# Copy PAXOS stuff to server
COPY ./paxos/*.java /usr/src/paxos/
# Change to top level directory where src folder is located
WORKDIR /usr/
# For logs
RUN mkdir logs
# Compile java files
RUN javac -d ./build ./src/*/*.java
# Change to the compiled binaries dir
WORKDIR /usr/build
# Start the rmi registry
CMD ["./rmiregistry"]

FROM bellsoft/liberica-openjdk-alpine-musl:11 AS client-build
COPY ./client/*.java /usr/src/client/

# Client needs server objects as well
COPY ./server/*.java /usr/src/server/
# Copy PAXOS stuff to server
COPY ./paxos/*.java /usr/src/paxos/

COPY ./client/*.properties /usr/config/
COPY ./client/default_data.txt /usr/config/
WORKDIR /usr/
# For logs
RUN mkdir logs
# Compile all java files
RUN javac -d ./build ./src/*/*.java

# Change to the compiled binaries dir
WORKDIR /usr/build