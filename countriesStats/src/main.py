from dataLoader import DataLoader


def main():
    dl = DataLoader("build_n_run.sh")
    dl.loadOutputStream()


if __name__ == "__main__":
    main()
