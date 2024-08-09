import matplotlib.pyplot as plt
import csv
import os

# Remove the largest outlier.
os.system('sort -h exact_times.csv | head --lines -1 > exact_times_without_outlier.csv')


def plot_values_from_file(file_name, title):
    reader = csv.reader(open(file_name))
    pairs = []
    for line in reader:
        pairs += [(int(line[0]), int(line[1]))]
    pairs.sort(key=lambda pair: pair[0])
    x = list(map(lambda p: p[0], pairs))
    y = list(map(lambda p: p[1], pairs))
    plt.plot(x, y, label=title)


plt.title('Comparison of the time taken by each algorithm')
plt.xlabel('Max counter upper bound')
plt.ylabel('Time taken to determine ambiguity (nano seconds)')
plot_values_from_file(
    'approx_times.csv',
    'Approximate analysis'
)
plot_values_from_file(
    'exact_times.csv',
    'Exact analysis'
)
plt.legend()
plt.show()

plt.title('Comparison of the time taken by each algorithm (with the outlier removed)')
plt.xlabel('Max counter upper bound')
plt.ylabel('Time taken to determine ambiguity (nano seconds)')
plot_values_from_file(
    'approx_times.csv',
    'Approximate analysis'
)
plot_values_from_file(
    'exact_times_without_outlier.csv',
    'Exact analysis (without outlier)'
)
plt.legend()
plt.show()
