FROM python:3.7.3-slim
MAINTAINER hellozjf
RUN pip install tensorflow flask tqdm
EXPOSE 5000

WORKDIR /tensorflow-params-transformer
ADD input_ids_mask_segment_label.py /tensorflow-params-transformer/input_ids_mask_segment_label.py
ADD tokenization.py /tensorflow-params-transformer/tokenization.py
ADD vocab.txt /tensorflow-params-transformer/vocab.txt

ENTRYPOINT ["python", "input_ids_mask_segment_label.py"]
