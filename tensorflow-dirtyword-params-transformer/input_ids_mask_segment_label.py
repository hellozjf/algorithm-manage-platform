import tensorflow as tf
import csv
import os
import tqdm
import tokenization
import collections
from flask import Flask
from flask import jsonify
from flask import request

app = Flask(__name__)


class DataProcessor(object):
    """Base class for data converters for sequence classification data sets."""

    def get_train_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the train set."""
        raise NotImplementedError()

    def get_dev_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the dev set."""
        raise NotImplementedError()

    def get_test_examples(self, data_dir):
        """Gets a collection of `InputExample`s for prediction."""
        raise NotImplementedError()

    def get_labels(self):
        """Gets the list of labels for this data set."""
        raise NotImplementedError()

    @classmethod
    def _read_tsv(cls, input_file, quotechar=None):
        """Reads a tab separated value file."""
        with tf.gfile.Open(input_file, "r") as f:
            reader = csv.reader(f, delimiter="\t", quotechar=quotechar)
            lines = []
            for line in reader:
                lines.append(line)
            return lines


class InputExample(object):
    """A single training/test example for simple sequence classification."""

    def __init__(self, guid, text_a, text_b=None, label=None):
        """Constructs a InputExample.

        Args:
          guid: Unique id for the example.
          text_a: string. The untokenized text of the first sequence. For single
            sequence tasks, only this sequence must be specified.
          text_b: (Optional) string. The untokenized text of the second sequence.
            Only must be specified for sequence pair tasks.
          label: (Optional) string. The label of the example. This should be
            specified for train and dev examples, but not for test examples.
        """
        self.guid = guid
        self.text_a = text_a
        self.text_b = text_b
        self.label = label


class myprocess(DataProcessor):
    def get_train_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the train set."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "train.tsv")), "train")

    def get_dev_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the dev set."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "dev.tsv")), "dev")

    def get_test_examples(self, data_dir):
        """Gets a collection of `InputExample`s for prediction."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "test.tsv")), "test")

    def get_labels(self):
        """See base class."""
        return ["0", "1"]

    def create_examples(self, lines, set_type, file_base=True):
        """Creates examples for the training and dev sets. each line is label+\t+text"""
        examples = []
        print(lines)
        for i, line in enumerate(lines):
            if file_base:
                if i == 0:
                    continue
            guid = "%s-%s" % (set_type, i)
            text = tokenization.convert_to_unicode(line[1])
            if set_type == "test" or set_type == "pred":
                label = "0"
            else:
                label = tokenization.convert_to_unicode(line[0])
            examples.append(
                InputExample(guid=guid, text_a=text, label=label))
        return examples


def convert_single_example(ex_index, example, label_list, max_seq_length, tokenizer):
    """Converts a single `InputExample` into a single `InputFeatures`."""

    label_map = {}
    for (i, label) in enumerate(label_list):
        label_map[label] = i

    tokens_a = tokenizer.tokenize(example.text_a)
    tokens_b = None
    if example.text_b:
        tokens_b = tokenizer.tokenize(example.text_b)

    if len(tokens_a) > max_seq_length - 2:
        tokens_a = tokens_a[0:(max_seq_length - 2)]

    tokens = []
    segment_ids = []
    tokens.append("[CLS]")
    segment_ids.append(0)
    for token in tokens_a:
        tokens.append(token)
        segment_ids.append(0)
    tokens.append("[SEP]")
    segment_ids.append(0)

    if tokens_b:
        for token in tokens_b:
            tokens.append(token)
            segment_ids.append(1)
        tokens.append("[SEP]")
        segment_ids.append(1)

    input_ids = tokenizer.convert_tokens_to_ids(tokens)

    # The mask has 1 for real tokens and 0 for padding tokens. Only real
    # tokens are attended to.
    input_mask = [1] * len(input_ids)

    # Zero-pad up to the sequence length.
    while len(input_ids) < max_seq_length:
        input_ids.append(0)
        input_mask.append(0)
        segment_ids.append(0)

    assert len(input_ids) == max_seq_length
    assert len(input_mask) == max_seq_length
    assert len(segment_ids) == max_seq_length

    label_id = label_map[example.label]
    print("input_ids", input_ids)
    print("input_mask", input_mask)
    print('segment_ids', segment_ids)
    print('label_id', label_id)

    return {
        'input_ids': input_ids,
        'input_mask': input_mask,
        'segment_ids': segment_ids,
        'label_id': label_id
    }


def convert_single_example2(ex_index, example, label_list, max_seq_length, tokenizer):
    """Converts a single `InputExample` into a single `InputFeatures`."""

    label_map = {}
    for (i, label) in enumerate(label_list):
        label_map[label] = i

    tokens_a = tokenizer.tokenize(example['text_a'])

    if len(tokens_a) > max_seq_length - 2:
        tokens_a = tokens_a[0:(max_seq_length - 2)]

    tokens = []
    segment_ids = []
    tokens.append("[CLS]")
    segment_ids.append(0)
    for token in tokens_a:
        tokens.append(token)
        segment_ids.append(0)
    tokens.append("[SEP]")
    segment_ids.append(0)

    input_ids = tokenizer.convert_tokens_to_ids(tokens)

    # The mask has 1 for real tokens and 0 for padding tokens. Only real
    # tokens are attended to.
    input_mask = [1] * len(input_ids)

    # Zero-pad up to the sequence length.
    while len(input_ids) < max_seq_length:
        input_ids.append(0)
        input_mask.append(0)
        segment_ids.append(0)

    assert len(input_ids) == max_seq_length
    assert len(input_mask) == max_seq_length
    assert len(segment_ids) == max_seq_length

    label_id = label_map[example['label']]
    print("input_ids", input_ids)
    print("input_mask", input_mask)
    print('segment_ids', segment_ids)
    print('label_ids', label_id)

    return {
        'input_ids': input_ids,
        'input_mask': input_mask,
        'segment_ids': segment_ids,
        'label_ids': label_id
    }


@app.route('/getParams', methods=['POST'])
def getParams():
    sentence = request.form['sentence']
    print('sentence:', sentence)
    example = {
        'label': '0',
        'text_a': sentence
    }

    max_seq_length = 128
    vocab_file = "vocab.txt"
    tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
    label_list = ['0']
    result = convert_single_example2('0', example, label_list, max_seq_length, tokenizer)

    return jsonify(result)


if __name__ == "__main__":
    app.run(host='0.0.0.0')
    # processor = myprocess()
    # predict_examples = processor.get_dev_examples('data')
    # max_seq_length=128
    # vocab_file="vocab.txt"
    # tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
    # label_list=['0','1']
    # for (ex_index, example) in enumerate(predict_examples):
    #     feature = convert_single_example(ex_index, example, label_list, max_seq_length, tokenizer)
