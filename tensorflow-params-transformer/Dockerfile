FROM 192.168.2.150/zrar/python:3.7.4-slim

ENV http_proxy http://192.168.2.149:8118
ENV https_proxy http://192.168.2.149:8118

MAINTAINER hellozjf
RUN apt-get update
RUN apt-get install -y build-essential
RUN pip install jieba numpy pandas tensorflow tqdm tokenization flask
EXPOSE 5000

WORKDIR /tensorflow-params-transformer
ADD input_ids_mask_segment_label.py /tensorflow-params-transformer/input_ids_mask_segment_label.py
ADD tokenization.py /tensorflow-params-transformer/tokenization.py
ADD vocab.txt /tensorflow-params-transformer/vocab.txt
ADD data /tensorflow-params-transformer/data

ENTRYPOINT ["python", "input_ids_mask_segment_label.py"]
