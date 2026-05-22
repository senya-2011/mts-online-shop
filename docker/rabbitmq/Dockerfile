FROM rabbitmq:3.13-management

RUN rabbitmq-plugins enable --offline rabbitmq_mqtt rabbitmq_prometheus

COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
