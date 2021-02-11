import React from "react";
import PropTypes from "prop-types";
import {View, StyleSheet, Text} from "react-native";
import CustomFontsProvider, { useCustomFont } from "react-native-custom-fonts";

const fontFaces = {
  // XXX: Specify the local name of your font. You'll use this to refer to it via the useCustomFont hook.
  'UbuntuBold': {
    uri: 'https://github.com/google/fonts/raw/master/ufl/ubuntu/Ubuntu-Bold.ttf',
    fontFamily: 'Ubuntu',
    fontWeight: 'bold',
    // XXX: You can also specify additional font styling.
    color: 'blue',
  },
};

const SomeComponent = () => {
  // Fetch the desired font by name. When the font has been cached, it will automatically update the View.
  const {...fontProps} = useCustomFont('UbuntuBold');
  return (
    <Text
      {...fontProps}
      children="Hello, world!"
    />
  );
};

export default () => (
  <CustomFontsProvider fontFaces={fontFaces}>
    <SomeComponent />
  </CustomFontsProvider>
);