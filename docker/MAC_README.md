# Run Oracle on macOS

This documents describes how to run Oracle-Database on macOS with Apple ARM processor using Docker.
Based on this guide: https://ronekins.com/2023/10/24/how-to-to-run-oracle-database-23c-free-on-m1-m2-apple-mac/

Install [Homebrew](https://brew.sh/) package manager. In order to check if Homebrew is installed, run the following command in the terminal:

```bash
$ brew --version
Homebrew 4.2.8
```

Install [Docker](https://www.docker.com/) container runtime and [Colima](https://github.com/abiosoft/colima/) container runtime. In order to check if Docker and Colima are installed, run the 
following command in the terminal:

```bash
$ brew install colima
...

$ colima --version
colima version 0.6.8

$ docker --version
Docker version 25.0.3, build 4debf41
```

Start the Colima container runtime using the `colima start` command using following options:

```bash
$ colima start \
    --arch x86_64 \
    --vm-type=vz \
    --vz-rosetta \
    --mount-type=virtiofs \
    --memory 8
INFO[0000] starting colima
INFO[0000] runtime: docker
INFO[0000] creating and starting ...
...
```

Confirm the Colima container runtime is running:

```bash
$ colima status
INFO[0000] colima is running using macOS Virtualization.Framework
INFO[0000] arch: x86_64
INFO[0000] runtime: docker
INFO[0000] mountType: virtiofs
INFO[0000] socket: unix:///Users/martin/.colima/default/docker.sock

$ colima list
PROFILE    STATUS     ARCH      CPUS    MEMORY    DISK     RUNTIME    ADDRESS
default    Running    x86_64    2       8GiB      60GiB    docker

$ docker context show
colima
```

Now you can run the database using `docker-compose up -d`. Docker-Desktop won't work. To show the database logs run:

```bash
$ docker logs task-app-trigger-oracle-db-1
```

To connect to the database use the following command:

```bash
$ docker exec -it task-app-trigger-oracle-db-1 sqlplus / as sysdba
```
