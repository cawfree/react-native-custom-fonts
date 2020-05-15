import React, {useEffect, useState, useRef, useCallback} from "react";
import PropTypes from "prop-types";
import {View, StyleSheet, Text, TextInput} from "react-native";
import CustomFontsProvider, {useCustomFont} from "react-native-custom-fonts";

const fontFaces = {
  'UbuntuBold': {
    uri: 'https://github.com/google/fonts/raw/master/ufl/ubuntu/Ubuntu-Bold.ttf',
    fontFamily: 'Ubuntu',
    fontWeight: 'bold',
    color: 'green',
  },
};

const SomeComponent = () => {
  const ref = useRef();
  const {...fontProps} = useCustomFont('UbuntuBold');
  return (
    <Text
      {...fontProps}
      children="Hello, world!"
    />
  );
};

export default () => {
  const [faces, setFaces] = useState(fontFaces);
  const onDownloadDidStart = useCallback(
    () => console.warn("Started download..."),
  );
  const onDownloadDidEnd = useCallback(
    () => console.warn("Download finished!"),
  );
  const onDownloadDidError = useCallback(
    e => console.error("Failed to download!", e),
  );
  useEffect(
    () => {
      setTimeout(
        () => {
          console.warn("Resetting...");
          setFaces({});
        },
        10000,
      );
    },
    [],
  );
  return (
    <CustomFontsProvider
      fontFaces={faces}
      onDownloadDidStart={onDownloadDidStart}
      onDownloadDidEnd={onDownloadDidEnd}
      onDownloadDidError={onDownloadDidError}
    >
      <SomeComponent
      />
    </CustomFontsProvider>
  );
};
