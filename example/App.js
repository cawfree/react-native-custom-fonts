import React from 'react';
import PropTypes from 'prop-types';
import {
  SafeAreaView,
  Button,
  Alert,
  Animated,
  TouchableOpacity,
  StyleSheet,
  View,
  Image,
  ActivityIndicator,
} from 'react-native';
import {
  CustomFontsProvider,
  Text,
  TextInput,
} from 'react-native-custom-fonts';

const styles = StyleSheet.create(
  {
    overlay: {
      position: 'absolute',
      top: 0,
      left: 0,
      bottom: 0,
      right: 0,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#222222',
    },
    container: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      padding: 15,
    },
    title: {
      fontSize: 22,
      fontWeight: 'bold',
      marginBottom: 5,
    },
    description: {
      textAlign: 'justify',
      fontSize: 16,
    },
    image: {
      width: 128,
      height: 128,
    },
    button: {
      margin: 5,
    },
  },
);

const getFontFamiliesFromFontFaces = (fontFaces = []) => fontFaces
  .reduce(
    (arr, { fontFamily }) => {
      if (arr.indexOf(fontFamily) < 0) {
        return [
          ...arr,
          fontFamily,
        ];
      }
      return arr;
    },
    [],
  );

const overlayOpacity = {
  visible: 0.9,
  hidden: 0.0,
};

const fontPacks = {
  abramov: [
    {
      fontFamily: 'Ubuntu',
      fontWeight: 'Normal',
      uri: 'https://raw.githubusercontent.com/opensourcedesign/fonts/master/ubuntu-font-family-0.80/Ubuntu-R.ttf',
    },
    {
      fontFamily: 'Ubuntu',
      fontWeight: 'Bold',
      uri: 'https://raw.githubusercontent.com/opensourcedesign/fonts/master/ubuntu-font-family-0.80/Ubuntu-B.ttf',
    },
    {
      fontFamily: 'Cabin Condensed',
      fontWeight: 'Normal',
      uri: 'https://raw.githubusercontent.com/google/fonts/master/ofl/cabincondensed/CabinCondensed-Regular.ttf',
    },
    {
      fontFamily: 'Cabin Condensed',
      fontWeight: 'Bold',
      uri: 'https://raw.githubusercontent.com/google/fonts/master/ofl/cabincondensed/CabinCondensed-Bold.ttf',
    }, 
  ],
  alpert: [
    {
      fontFamily: 'Lobster Two',
      fontWeight: 'Normal',
      uri: 'https://raw.githubusercontent.com/google/fonts/master/ofl/lobstertwo/LobsterTwo-Regular.ttf',
    },
    {
      fontFamily: 'Lobster Two',
      fontWeight: 'Bold',
      uri: 'https://raw.githubusercontent.com/google/fonts/master/ofl/lobstertwo/LobsterTwo-Bold.ttf',
    },
  ],
};

export default class App extends React.Component {
  state = {
    fontPack: 'abramov',
    fontFamily: fontPacks.abramov[0].fontFamily,
    titleSize: 50,
    descriptionSize: 20,
    fontFaces: fontPacks.alpert,
    animOverlay: new Animated.Value(overlayOpacity.visible),
    loading: true,
  }
  componentWillUpdate(nextProps, nextState) {
    const {
      loading,
      animOverlay,
    } = nextState;
    if (loading !== this.state.loading) {
      const toValue = loading ? overlayOpacity.visible : overlayOpacity.hidden;
      Animated.timing(
        animOverlay,
        {
          duration: 1000,
          toValue,
          useNativeDriver: true,
        },
      )
        .start();
    }
  }
  changeFontPacks = () => new Promise((resolve) => {
    const {
      fontPack: oldFontPack,
    } = this.state;
    const fontPack = oldFontPack === 'abramov' ? 'alpert' : 'abramov';
    this.setState(
      {
        fontPack,
        fontFamily: fontPacks[fontPack][0].fontFamily,
      },
    );
  });
  onDownloadDidStart = () => {
    this.setState(
      {
        loading: true,
      },
    );
  }
  onDownloadDidEnd = () => {
    this.setState(
      {
        loading: false,
      },
    );
  }
  render() {
    const {
      fontFamily,
      titleSize,
      descriptionSize,
      animOverlay,
      loading,
      fontPack,
    } = this.state;
    const fontFaces = fontPacks[fontPack];
    return (
      <SafeAreaView
        style={{
          flex: 1,
        }}
      >
      <CustomFontsProvider
        fontFaces={fontFaces}
        onDownloadDidStart={this.onDownloadDidStart}
        onDownloadDidEnd={this.onDownloadDidEnd}
      >
        <View
          style={styles.container}
        >
          <Image
            style={styles.image}
            source={{
              uri: 'https://cdn4.iconfinder.com/data/icons/logos-3/600/React.js_logo-512.png',
            }}
          />
          <TextInput
            style={{
              fontWeight: 'bold',
              fontSize: titleSize,
              fontFamily,
              marginBottom: 5,
              textAlign: 'center',
            }}
            multiline
            placeholder="Welcome to React Native!"
          />
            <Text
              style={{
                fontSize: descriptionSize,
                fontWeight: 'normal',
                fontFamily,
                textAlign: 'justify',
              }}
            >
              {'You may not believe it, but there are zero custom font files stored inside the application bundle! Instead, everything you see is brought across from the web.'}
            </Text>
          <Animated.View
            style={{
              position: 'absolute',
              flexDirection: 'row',
              bottom: 0,
              opacity: Animated.subtract(
                1,
                animOverlay,
              ),
            }}
          >
            {getFontFamiliesFromFontFaces(fontFaces).map(selectedFontFamily => (
              <View
                key={selectedFontFamily}
                style={{
                  padding: 15,
                }}
              >
                <Button
                  color={(fontFamily === selectedFontFamily) ? 'firebrick' : 'blue'}
                  title={selectedFontFamily}
                  onPress={() => this.setState({
                    fontFamily: selectedFontFamily,
                  })}
                />
              </View>
            ))}
          </Animated.View>
          <Animated.View
            style={{
              position: 'absolute',
              top: 0,
              flexDirection: 'row',
              height: 40,
              opacity: Animated.subtract(
                1,
                animOverlay,
              ),
            }}
          >
            <Button
              onPress={() => this.setState({
                titleSize: this.state.titleSize + 5,
              })}
              title="Title +"
            />
            <Button
              onPress={() => this.setState({
                titleSize: this.state.titleSize - 5,
              })}
              title="Title -"
            />
            <Button
              onPress={() => this.setState({
                descriptionSize: this.state.descriptionSize + 5,
              })}
              title="Desc +"
            />
            <Button
              onPress={() => this.setState({
                descriptionSize: this.state.descriptionSize - 5,
              })}
              title="Desc -"
            />
            <Button
              onPress={this.changeFontPacks}
              title={fontPack}
              color={fontPack === 'abramov' ? 'orange' : 'teal'}
            />
          </Animated.View>
        </View>
        <Animated.View
          pointerEvents={loading ? 'auto' : 'none'}
          style={[
            styles.overlay,
            {
              opacity: animOverlay,
              flexDirection: 'row',
            },
          ]}
        >
          <ActivityIndicator
          />
          <Text
            style={{
              marginLeft: 15,
              fontSize: 20,
              color: 'white',
            }}
          >
            {'Loading fonts...'}
          </Text>
        </Animated.View>
      </CustomFontsProvider>
      </SafeAreaView>
    );
  }
};
